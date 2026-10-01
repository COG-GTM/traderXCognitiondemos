#!/usr/bin/env python3
"""Demo 2, step 1 — inventory every settlement-cycle assumption in the estate, with file:line citations,
CODEOWNERS ownership, a classification, and a tranche assignment.

    python3 demo/t1/inventory.py            # -> demo/out/t1/inventory.json, demo/reports/demo2-t1-inventory.md,
                                            #    demo/reports/demo2-t1-migration-plan.md

Scans the *workspace* (what a Devin session would clone), not the pristine repos/ tree.  Detection is a rule set
(regexes for lags, cycle literals, cut-off wording, DATEADD, cron comments).  Judgement — what each hit *means*,
which tranche it belongs to, and which hits are business decisions rather than code changes — lives in ASSESSMENTS
so the demo is deterministic and every call is written down where a reviewer can disagree with it.
"""
from __future__ import annotations

import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, REPORTS, WORKSPACE, log, now_iso, owners_for, write_json  # noqa: E402

GO_LIVE = "2027-10-11"
PROGRAMME = "SETTLE-4471 (EU/UK/CH T+1, go-live 11 Oct 2027)"

SKIP_DIRS = {".git", "target", "node_modules", "build", "dist", ".seed", "__pycache__"}
SKIP_FILES = {"CHANGELOG.md", "package-lock.json"}
TEXT_EXT = {".java", ".py", ".js", ".ts", ".yml", ".yaml", ".sql", ".jil", ".tf", ".md", ".properties", ".txt", ".xml", ".conf"}

# (regex, kind) — first match wins.  Deliberately narrow: an inventory a reviewer can trust beats a long one.
RULES: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"\b(SETTLE_DAYS|DEFAULT_LAG|GIVE_UP_SETTLEMENT_DAYS|FX_SPOT_DAYS|RECALL_NOTICE_BUSINESS_DAYS|EX_DATE_OFFSET_BUSINESS_DAYS)\s*=\s*\d"), "hard-coded lag"),
    (re.compile(r'"(EU|UK|CH|JP)":\s*2'), "hard-coded lag"),
    (re.compile(r"\bT_PLUS_2\b"), "cycle literal"),
    (re.compile(r"\bT\+2\b"), "cycle literal"),
    (re.compile(r"DATEADD\(day,\s*2"), "hard-coded lag"),
    (re.compile(r"default_fix_settl_type: \"3\"|fix_settl_type: \"3\""), "wire mapping"),
    (re.compile(r"12:00 (on )?T\+1|T\+1 12:00"), "operational cut-off"),
    (re.compile(r"two business days|2 business days"), "prose assumption"),
]

# repo -> path -> (tranche, action, decision_owner or None)
#   tranche 1: instruction-generation path (what the golden file replays)          — executed by demo/t1/migrate.py
#   tranche 2: downstream consumers, schedulers, recon, client-facing wording       — planned, one PR per repo
#   tranche 3: needs a business decision before code changes                        — escalated, not coded
ASSESSMENTS: dict[str, dict[str, tuple[int, str, str | None]]] = {
    "core-dates": {
        "src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java": (1, "Date-gate standard(): T+2 before go-live, T+1 from 11 Oct 2027; add standard(LocalDate) / forMarket(mic, date). Keep the no-arg forms deprecated so the compiler flags remaining callers.", None),
        "src/main/java/com/ardencm/posttrade/dates/SettlementDates.java": (1, "Resolve cycle from trade date, not a static default.", None),
        "src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java": (1, "No change; inherits the date-gated default. Used by the parity harness.", None),
        "src/main/java/com/ardencm/posttrade/dates/FixDates.java": (1, "No change — hit is the FIX tag 63 code table (3 = T+2), which must keep all values. False positive, kept so the reviewer sees it was considered.", None),
        "src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java": (1, "Existing T+2 expectations become pre-go-live cases; add post-go-live cases incl. the 8→12 Oct double-settlement day.", None),
        "docs/t1-calendar-mismatch.md": (3, "Venue vs TARGET2 calendar divergence under T+1 (Whit Monday, UK bank holidays, Boxing Day substitute). Which calendar is authoritative is not a code decision.", "@ardencm/market-ops + Head of Post-Trade"),
        "src/main/resources/holidays.yaml": (3, "Calendar data is correct; the *rule* for divergent dates is the open question above.", "@ardencm/market-ops"),
    },
    "settlement-instruction-service": {
        "src/main/resources/application.yml": (1, "Drop the explicit XLON/XPAR/XETR/XSWX T_PLUS_2 pins so venues fall back to the date-gated default; keep North America explicit.", None),
        "src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java": (1, "cycleFor(mic, tradeDate) — fallback is SettlementCycle.standard(tradeDate).", None),
        "src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java": (1, "Pass trade date into cycleFor; tag 63 follows the resolved cycle (3→2).", None),
        "src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java": (1, "Split into pre/post go-live cases; contract tests (tag 64 LocalMktDate) unchanged.", None),
        "src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java": (1, "No change — this is the consumer contract (tag 64 yyyyMMdd) the migration must keep green.", None),
    },
    "iso20022-fix-messages": {
        "mapping/settlement-cycle.yaml": (1, "EU/UK/CH markets → cycle T+1, tag 63 = 2, effective 11 Oct 2027. Custodians consume this file — the change *is* the counterparty notice.", None),
        "README.md": (1, "Update wording.", None),
        "mapping/archive/settlement-cycle-pre-t1.yaml": (1, "No change — this is the pre-T+1 mapping archived by tranche 1 so custodians can diff old vs new.", None),
    },
    "batch-scheduler-config": {
        "calendars/settlement-cutoffs.yaml": (2, "Affirmation and CSD-matching cut-offs move from 12:00 T+1 to T evening (venue-specific; ops to confirm exact times with each CSD).", None),
        "jil/settlements.jil": (2, "SETTLE_INSTR_GEN must run on T (evening) not T+1 06:00; AFFIRMATION_CHASER on T; RECALL_SWEEP needs re-timing (see stock-loan decision).", None),
    },
    "platform-terraform": {
        "batch/schedules.tf": (2, "EventBridge cron for instruction generation moves to T evening to match the JIL change.", None),
    },
    "recon-job": {
        "recon/expected.py": (2, "DEFAULT_LAG 2→ date-gated 1; LATE_MATCH_TOLERANCE_DAYS of 1 becomes 100% of the cycle — recon ops to decide whether 'late match' survives.", None),
    },
    "risk-lib": {
        "risk_lib/settlement_exposure.py": (2, "EU/UK/CH lag 2→1 from go-live (region-level; JP stays 2). Days-at-risk and exposure figures halve — risk to re-baseline limits.", None),
    },
    "allocation-service": {
        "src/main/java/com/ardencm/posttrade/allocation/AllocationService.java": (2, "GIVE_UP_SETTLEMENT_DAYS uses plusDays (calendar days, not business days) — latent bug independent of T+1; route through core-dates.", None),
        "src/main/resources/application.yml": (2, "default-cycle T+2 → T+1 for client confirm wording.", None),
    },
    "confirmation-service": {
        "src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java": (2, "Replace `mic.startsWith(\"XN\") ? T_PLUS_1 : T_PLUS_2` with core-dates forMarket(mic, tradeDate).", None),
        "src/main/resources/application.yml": (2, "Client narrative and footer text state T+2 and a 12:00 T+1 affirmation deadline.", None),
        "src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java": (2, "Test fixture narrative hard-codes 'T+2 basis'; becomes a pre-go-live case.", None),
    },
    "custody-gateway": {
        "src/main/resources/application.yml": (2, "Comment documents the T+1 12:00 CET matching cut-off for T+2 settlement; cut-off moves to T.", None),
    },
    "client-portal": {
        "src/server.js": (2, "Client-facing FAQ string says trades settle T+2.", None),
    },
    "legacy-stored-procs": {
        "procs/usp_FlagLateSettlements.sql": (2, "DATEADD(day, 2, TradeDate) → 1 from go-live; feeds the CSDR penalty report, so the change must be dated not flipped.", None),
        "views/vw_SettlementCalendar.sql": (2, "NaiveSettlementDate = DATEADD(day, 2, …) drives the legacy client statement; date-gate with CASE WHEN TradeDate >= '2027-10-11'.", None),
        "README.md": (2, "README enumerates the three lag-bearing objects; no automated tests — DBA smoke only, so the PR needs a UAT ticket.", None),
    },
    "legacy-position-keeper": {
        "src/com/arden/poskeeper/SettleDateUtil.java": (3, "SETTLE_DAYS = 2 in a JDK 8 Ant build that cannot take core-dates (PK-118); original author left 2018. Needs an owner and a decision: patch the constant with a date gate, or retire the service.", "@ardencm/legacy-platform + Head of Post-Trade"),
    },
    "corporate-actions-service": {
        "src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java": (3, "Ex-date = record date under T+1, but CA-2291 is open: events announced before go-live with record dates after it, and dual-listed XSWX/XLON names. Do not change until asset-servicing ops rules.", "asset-servicing ops (CA-2291)"),
        "docs/ex-date-policy.md": (3, "Policy document records the open question.", "asset-servicing ops (CA-2291)"),
    },
    "fx-funding-service": {
        "src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java": (3, "FX spot stays T+2 (market convention, not CSDR). Under T+1 every cross-currency trade needs tom-next or pre-funding — treasury policy, not a code fix.", "treasury"),
        "README.md": (3, "Documents the convention above.", "treasury"),
    },
    "stock-loan-recall-service": {
        "src/main/java/com/ardencm/seclending/recall/RecallService.java": (3, "GMSLA 2-business-day recall notice makes the deadline fall *before* trade date under T+1. Notice period is contractual — legal/seclending to renegotiate or accept fails.", "securities lending + legal"),
        "README.md": (3, "States the notice period is a legal-agreement change.", "securities lending + legal"),
    },
}

TRANCHE_TITLES = {
    1: "Instruction-generation path (core-dates → settlement-instruction-service → message mapping)",
    2: "Downstream consumers, schedulers, recon, client-facing wording",
    3: "Business decisions required before code changes",
}


def scan_repo(repo: pathlib.Path) -> list[dict]:
    hits = []
    for p in sorted(repo.rglob("*")):
        if not p.is_file() or p.suffix not in TEXT_EXT or p.name in SKIP_FILES:
            continue
        if any(part in SKIP_DIRS for part in p.relative_to(repo).parts):
            continue
        rel = str(p.relative_to(repo))
        try:
            lines = p.read_text(errors="replace").splitlines()
        except OSError:
            continue
        for n, line in enumerate(lines, 1):
            for rx, kind in RULES:
                if rx.search(line):
                    hits.append({"path": rel, "line": n, "kind": kind, "snippet": line.strip()[:140]})
                    break
    return hits


def main() -> None:
    inventory = []
    for repo_dir in sorted(WORKSPACE.iterdir()):
        if not (repo_dir / ".git").exists():
            continue
        assessments = ASSESSMENTS.get(repo_dir.name, {})
        hits = scan_repo(repo_dir)
        by_path: dict[str, list[dict]] = {}
        for h in hits:
            by_path.setdefault(h["path"], []).append(h)
        for path, phits in by_path.items():
            tranche, action, decision = assessments.get(path, (0, "UNASSESSED — review manually", None))
            inventory.append({
                "repo": repo_dir.name, "path": path, "owners": owners_for(repo_dir, path),
                "hits": phits, "kinds": sorted({h["kind"] for h in phits}),
                "tranche": tranche, "action": action, "decision_owner": decision,
                "is_test": "/test/" in path or path.startswith("tests/"),
            })
        for path in assessments:
            if path not in by_path:
                inventory.append({"repo": repo_dir.name, "path": path, "owners": owners_for(repo_dir, path), "hits": [],
                                  "kinds": [], "tranche": assessments[path][0], "action": assessments[path][1],
                                  "decision_owner": assessments[path][2], "is_test": False})

    unassessed = [i for i in inventory if i["tranche"] == 0]
    summary = {
        "programme": PROGRAMME, "go_live": GO_LIVE, "generated_at": now_iso(),
        "repos_scanned": sum(1 for d in WORKSPACE.iterdir() if (d / ".git").exists()),
        "repos_affected": len({i["repo"] for i in inventory}), "files": len(inventory),
        "citations": sum(len(i["hits"]) for i in inventory),
        "by_tranche": {t: len([i for i in inventory if i["tranche"] == t]) for t in (1, 2, 3)},
        "unassessed": len(unassessed),
        "decisions": sorted({i["decision_owner"] for i in inventory if i["decision_owner"]}),
    }
    write_json(OUT / "t1/inventory.json", {"summary": summary, "items": inventory})
    REPORTS.mkdir(parents=True, exist_ok=True)
    (REPORTS / "demo2-t1-inventory.md").write_text(render_inventory(summary, inventory))
    (REPORTS / "demo2-t1-migration-plan.md").write_text(render_plan(summary, inventory))
    log(f"inventory: {summary['files']} files / {summary['citations']} citations across {summary['repos_affected']} repos; "
        f"tranches {summary['by_tranche']}; unassessed {summary['unassessed']} -> {OUT / 't1/inventory.json'}")
    for i in unassessed:
        log(f"  UNASSESSED {i['repo']}/{i['path']}")


def render_inventory(s: dict, inv: list[dict]) -> str:
    out = [f"# T+1 settlement-cycle inventory — {s['programme']}", "",
           f"Generated {s['generated_at']} by `demo/t1/inventory.py` over {s['repos_scanned']} repositories in the Arden Capital Markets "
           f"estate (synthetic). {s['files']} files in {s['repos_affected']} repos carry a settlement-cycle assumption; "
           f"{s['citations']} line citations. Every row below is a claim a reviewer can check by opening the file.", "",
           "| Repo | File | Line(s) | What | Owners (CODEOWNERS) | Tranche |", "|---|---|---|---|---|---|"]
    for i in sorted(inv, key=lambda x: (x["tranche"] or 9, x["repo"], x["path"])):
        lines = ", ".join(str(h["line"]) for h in i["hits"]) or "—"
        out.append(f"| {i['repo']} | `{i['path']}` | {lines} | {', '.join(i['kinds']) or 'policy/doc'} | {', '.join(i['owners']) or '—'} | {i['tranche'] or '?'} |")
    out += ["", "## Citations", ""]
    for i in sorted(inv, key=lambda x: (x["repo"], x["path"])):
        if not i["hits"]:
            continue
        out.append(f"### {i['repo']}/{i['path']}")
        for h in i["hits"]:
            out.append(f"- L{h['line']} ({h['kind']}): `{h['snippet']}`")
        out.append("")
    return "\n".join(out)


def render_plan(s: dict, inv: list[dict]) -> str:
    out = [f"# T+1 migration plan — {s['programme']}", "",
           f"Go-live **{s['go_live']}** (EU, UK, Switzerland; US/CA already T+1 since 28 May 2024). Plan generated {s['generated_at']} "
           f"from the inventory in `demo2-t1-inventory.md`. Three tranches: code the instruction path first (that is what counterparties "
           f"see), then consumers, and park anything that is a business decision until the named owner rules.", "",
           "Principle: **date-gate, don't flip.** Every change resolves the cycle from the trade date so pre-go-live trades still "
           "settle T+2, back-dated corrections keep working, and the same build runs before and after 11 Oct 2027.", ""]
    for t in (1, 2, 3):
        items = [i for i in inv if i["tranche"] == t]
        out += [f"## Tranche {t} — {TRANCHE_TITLES[t]}", ""]
        if t == 1:
            out += ["Executed by `demo/t1/migrate.py --tranche 1`; verified by the golden-file parity harness (`demo/t1/parity.py`). One PR per repo.", ""]
        if t == 2:
            out += ["One PR per repo, same date-gating pattern, each with its own repo tests. Scheduler/IaC changes need a CAB ticket. Not executed in this demo.", ""]
        if t == 3:
            out += ["Not coded. Each item is routed to a named human owner with the question written out; engineering re-enters once a decision exists.", ""]
        repos = sorted({i["repo"] for i in items})
        for r in repos:
            out.append(f"### {r}")
            for i in [x for x in items if x["repo"] == r]:
                cite = f"`{i['path']}`" + (f" L{i['hits'][0]['line']}" if i["hits"] else "")
                owners = ", ".join(i["owners"]) or "—"
                out.append(f"- {cite} — {i['action']}  \n  owners: {owners}" + (f" · **decision: {i['decision_owner']}**" if i["decision_owner"] else ""))
            out.append("")
    out += ["## Open decisions (human-owned)", ""]
    for d in s["decisions"]:
        qs = [f"{i['repo']}/{i['path'].split('/')[-1]}" for i in inv if i["decision_owner"] == d]
        out.append(f"- **{d}** — {', '.join(sorted(set(qs)))}")
    out += ["", "## Verification", "",
            "- Golden file `demo/t1/golden/trades-2027Q4.csv` replayed through the pre- and post-migration `core-dates` CLI; every difference is listed and classified (expected / unchanged / escalate).",
            "- Repo unit tests and `*ContractTest` per PR (tag 64 stays `yyyyMMdd`; tag 63 moves 3→2 only for post-go-live EU/UK/CH trades).",
            "- Calendar-divergence trades are reported, not resolved (see `core-dates/docs/t1-calendar-mismatch.md`).", ""]
    return "\n".join(out)


if __name__ == "__main__":
    main()
