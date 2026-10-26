#!/usr/bin/env python3
"""SETTLE-4471 inventory: every settlement-cycle assumption in ~/finserv-workspace, with CODEOWNERS and a tranche."""
import json, pathlib, re, subprocess, sys
sys.path.insert(0, str(pathlib.Path.home() / "repos/traderXCognitiondemos/arden/finserv-estate/demo"))
from estatelib import owners_for  # noqa: E402

WS = pathlib.Path.home() / "finserv-workspace"
SKIP_PARTS = {".git", "node_modules", "target", ".seed"}
SKIP_FILES = {"package-lock.json", "dependency-reduced-pom.xml"}
PATTERNS = [  # (kind, regex)
    ("hard-coded lag", r"T_PLUS_2|\b(SETTLE_DAYS|SETTLEMENT_LAG_DAYS|DEFAULT_LAG|GIVE_UP_SETTLEMENT_DAYS|EX_DATE_OFFSET_BUSINESS_DAYS)\b|\"(EU|UK|CH|JP)\": 2\b|@Lag INT = 2"),
    ("hard-coded lag", r"plusDays\((GIVE_UP|2\))|DATEADD\(day, 2"),
    ("hard-coded lag", r"\bstandard\(\)|forMarket\(mic\)|cycleFor\(mic\)|getOrDefault\(mic"),
    ("market-default caller", r"SettlementDates\.settlementDate\(\w+(\(\))?, \w+(\(\))?\)"),
    ("mapping", r"T\+2|default[_-]cycle|fix_settl_type: \"3\"|63=3|return \"3\""),
    ("schedule/cut-off", r"T\+1 12:00|12:00 T\+1|12:00 on T\+1|on T\+1\.|cutoff-offset-days|T-1 trades|trades dated T-1|start_times: \"(06:00|09:00|15:30)\"|cron\(0 6 "),
    ("FX spot", r"FX_SPOT_DAYS|(?i:spot value date)"),
    ("recall notice", r"RECALL_NOTICE_BUSINESS_DAYS|(?i:recall notice period)|2 business days\)"),
    ("ex-date", r"(?i:ex-date =|record date − 1|record date minus|dual-listing-calendar-policy|use the primary listing's calendar)"),
    ("wording", r"(?i:two business days|2 business days after)"),
    ("test pin", r"2027, ?10, ?13|20271013|2027-10-13|2027, 10, 14\)\);|TPlusTwo|TwoBusinessDays|days_at_risk == 2"),
]
RX = [(k, re.compile(p)) for k, p in PATTERNS]

# (repo, path) -> (tranche, action/reason, decision owner for tranche 3)
A = {
 # ---- tranche 1: instruction-generation path
 ("core-dates", "src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java"): ("1", "Add forMarket(mic, tradeDate) / standard(tradeDate) gated on 2027-10-11 for XLON/XPAR/XETR/XSWX; keep standard()/forMarket(mic) returning T+2 and deprecate", ""),
 ("core-dates", "src/main/java/com/ardencm/posttrade/dates/SettlementDates.java"): ("1", "settlementDate(tradeDate, mic) resolves the cycle from the trade date; venue calendar kept (calendar authority escalated)", ""),
 ("core-dates", "src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java"): ("1", "CLI default cycle from forMarket(mic, tradeDate)", ""),
 ("core-dates", "src/main/java/com/ardencm/posttrade/dates/FixDates.java"): ("1", "No change on the 1.4 line: tag 63 mapping already has T_PLUS_1 -> 2; tag 64/75 must stay yyyyMMdd (1.5.0 on main broke this)", ""),
 ("core-dates", "src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java"): ("1", "Post-go-live pins move to T+1; pre-go-live T+2 cases added; tag 64 yyyyMMdd assertion kept", ""),
 ("core-dates", "README.md"): ("1", "Document date-gated default", ""),
 ("core-dates", "CHANGELOG.md"): ("1", "1.4.2 entry", ""),
 ("core-dates", "src/main/resources/holidays.yaml"): ("3", "No change. Venue vs TARGET2/cash calendar divergence becomes the settlement date under T+1; which calendar is authoritative is a business decision", "@ardencm/market-ops + Head of Post-Trade"),
 ("core-dates", "docs/t1-calendar-mismatch.md"): ("3", "Open question doc; no change", "@ardencm/market-ops + Head of Post-Trade"),
 ("settlement-instruction-service", "src/main/resources/application.yml"): ("1", "Drop explicit XLON/XPAR/XETR/XSWX T_PLUS_2 pins so the date gate applies (market-ops sign-off per OPS-RUNBOOK-14)", ""),
 ("settlement-instruction-service", "src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java"): ("1", "Add cycleFor(mic, tradeDate); explicit pins still win; deprecate cycleFor(mic)", ""),
 ("settlement-instruction-service", "src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java"): ("1", "Resolve cycle with the trade date", ""),
 ("settlement-instruction-service", "src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java"): ("1", "Existing pinned-T+2 cases kept; add unpinned pre/post go-live cases", ""),
 ("settlement-instruction-service", "src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java"): ("none", "Consumer contract (custody-gateway): keep green, do not edit. Its examples pin XLON T_PLUS_2 explicitly and check tag 64 = yyyyMMdd", ""),
 ("settlement-instruction-service", "src/test/resources/contracts/custody-gateway.json"): ("none", "Consumer-owned contract copy; a T+1 example needs a contract bump by @ardencm/custody-integration (tranche 2 follow-up)", ""),
 ("settlement-instruction-service", "pom.xml"): ("1", "Override core-dates to 1.4.2 in the service's dependencyManagement (BOM pin not moved)", ""),
 ("iso20022-fix-messages", "mapping/settlement-cycle.yaml"): ("1", "EU/UK/CH entries become T+1 / 63=2 with effective_from 2027-10-11 and previous_* values; pre-migration file archived", ""),
 ("iso20022-fix-messages", "validate.py"): ("1", "Pick the mapping entry by tag 75 trade date vs effective_from", ""),
 ("iso20022-fix-messages", "samples/fix/AE-xlon-regular-way.fix"): ("1", "Golden sample (trade date 2027-10-11) becomes T+1: 63=2, 64=20271012; old sample archived; pre-go-live sample added", ""),
 ("iso20022-fix-messages", "samples/iso20022/sese.023-xlon.xml"): ("1", "SttlmDt 2027-10-13 -> 2027-10-12; old sample archived", ""),
 ("iso20022-fix-messages", "tests/test_validate.py"): ("1", "Validate every sample, incl. archive against the archived mapping", ""),
 # ---- tranche 2: downstream consumers, schedules, wording
 ("allocation-service", "src/main/resources/application.yml"): ("2", "default-cycle: T+2 for client confirms; date-gate with post-trade ops", ""),
 ("allocation-service", "src/main/java/com/ardencm/posttrade/allocation/AllocationService.java"): ("2", "Give-ups use tradeDate.plusDays(2) calendar days (ops request 2019) and ignore holidays/cycle; replace with core-dates", ""),
 ("allocation-service", "src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java"): ("2", "Test pins T+2 dates; update with the service change", ""),
 ("confirmation-service", "src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java"): ("2", "tag 63 hard-coded T_PLUS_2 for non-XN venues while settlement date comes from core-dates: once the BOM moves, confirms would say T+1 date with 63=3. Must change before the BOM pin moves", ""),
 ("confirmation-service", "src/main/resources/application.yml"): ("2", "Client narrative/PDF footer say T+2 / two business days", ""),
 ("confirmation-service", "src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java"): ("2", "Test pins T+2 for a 2027-10-11 trade", ""),
 ("custody-gateway", "src/main/resources/application.yml"): ("2", "Matching cut-off comment/offset keyed to T+2 (T+1 12:00); under T+1 the cut-off is T or S-0", ""),
 ("custody-gateway", "contracts/settlement-instruction-service.json"): ("2", "Contract examples are T+2; add T+1 example with a version bump (owner: custody-integration). Tag 64 pattern unchanged", ""),
 ("batch-scheduler-config", "jil/settlements.jil"): ("2", "SETTLE_INSTR_GEN 06:00 next morning and AFFIRMATION_CHASER 09:00 T+1 assume T+2; must move to trade date evening. RECALL_SWEEP wording", ""),
 ("batch-scheduler-config", "calendars/settlement-cutoffs.yaml"): ("2", "EU/UK/CH cut-offs 12:00 T+1, cycle T+2", ""),
 ("platform-terraform", "batch/schedules.tf"): ("2", "EventBridge settle-instr-gen cron 06:00 mirrors Autosys (T+2)", ""),
 ("recon-job", "recon/expected.py"): ("2", "DEFAULT_LAG = 2 for every non-NA venue; ignores holidays; would flag every T+1 settlement as early/BREAK", ""),
 ("recon-job", "tests/test_expected.py"): ("2", "Test pins T+2", ""),
 ("risk-lib", "risk_lib/settlement_exposure.py"): ("2", "SETTLEMENT_LAG_DAYS EU/UK/CH = 2 (regions; no date gate). JP stays 2", ""),
 ("risk-lib", "tests/test_settlement_exposure.py"): ("2", "Tests pin EU T+2 and days_at_risk == 2", ""),
 ("legacy-stored-procs", "procs/usp_CalcSettlementDate.sql"): ("2", "@Lag default 2 (London book); Settlements sign-off; DBA change ticket", ""),
 ("legacy-stored-procs", "procs/usp_FlagLateSettlements.sql"): ("2", "Hard-coded DATEADD(day, 2) feeds CSDR penalty report; calendar days, not business days", ""),
 ("legacy-stored-procs", "views/vw_SettlementCalendar.sql"): ("2", "NaiveSettlementDate DATEADD(day, 2) for the legacy client statement", ""),
 ("legacy-stored-procs", "tests/smoke.sql"): ("2", "Smoke expects 2027-10-13", ""),
 ("legacy-stored-procs", "README.md"): ("2", "Docs describe the three lag-carrying objects", ""),
 ("risk-lib", "README.md"): ("2", "Documents SETTLEMENT_LAG_DAYS (RISK-2210: move to core-dates calendars)", ""),
 ("legacy-position-keeper", "src/com/arden/poskeeper/SettleDateUtil.java"): ("2", "SETTLE_DAYS = 2, JDK 8, cannot take core-dates (PK-118); needs a date-gated local change", ""),
 ("client-portal", "src/server.js"): ("2", "/api/settlement/summary says 'Trades settle 2 business days after execution (T+2)'", ""),
 # ---- tranche 3: business decisions
 ("corporate-actions-service", "src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java"): ("3", "EX_DATE_OFFSET_BUSINESS_DAYS = 1 (T+2 regime). Regime for events announced before / record after go-live and dual listings is open (CA-2291)", "Asset-servicing ops, CA-2291"),
 ("corporate-actions-service", "src/test/java/com/ardencm/assetservicing/corpactions/ExDateCalculatorTest.java"): ("3", "Tests pin T+2 ex-date", "Asset-servicing ops, CA-2291"),
 ("corporate-actions-service", "src/main/resources/application.yml"): ("3", "Dual-listing calendar policy = primary; dual-listed XSWX/XLON ex-date unresolved", "Asset-servicing ops, CA-2291"),
 ("corporate-actions-service", "docs/ex-date-policy.md"): ("3", "Policy doc with the open question", "Asset-servicing ops, CA-2291"),
 ("fx-funding-service", "src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java"): ("3", "FX_SPOT_DAYS = 2 is FX market convention (not CSDR); under T+1 every EUR/GBP/CHF funding becomes tom-next or pre-funded. Funding approach is a treasury decision", "Treasury (FX funding), via @ardencm/treasury-tech"),
 ("fx-funding-service", "README.md"): ("3", "Documents spot T+2 convention", "Treasury (FX funding), via @ardencm/treasury-tech"),
 ("fx-funding-service", "src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java"): ("3", "Test pins T+2 securities settlement", "Treasury (FX funding), via @ardencm/treasury-tech"),
 ("stock-loan-recall-service", "src/main/java/com/ardencm/seclending/recall/RecallService.java"): ("3", "RECALL_NOTICE_BUSINESS_DAYS = 2 from GMSLA schedules; under T+1 the recall deadline falls before trade date. Legal-agreement change", "Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending"),
 ("stock-loan-recall-service", "README.md"): ("3", "Notice period is a legal term", "Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending"),
 ("stock-loan-recall-service", "src/test/java/com/ardencm/seclending/recall/RecallServiceTest.java"): ("3", "Test pins T+2 recall deadline", "Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending"),
}
NO_CHANGE = {  # false positives / out-of-scope with reason
 ("custody-gateway", "src/test/java/com/ardencm/custody/gateway/FixSettlDateParserTest.java"): "False positive for the cycle: tests tag 64 parsing format (yyyyMMdd), which stays the contract",
 ("core-dates", "src/main/java/com/ardencm/posttrade/dates/BusinessCalendar.java"): "Generic calendar arithmetic, no cycle assumption",
}

def ref(repo):
    """Scan the pre-migration tree (main), not the tranche branch checked out in the workspace."""
    ok = subprocess.run(["git", "-C", str(WS / repo), "rev-parse", "--verify", "-q", "main"], capture_output=True).returncode == 0
    return "main" if ok else "HEAD"

def read(repo, f):
    return subprocess.run(["git", "-C", str(WS / repo), "show", f"{ref(repo)}:{f}"], capture_output=True, check=True).stdout.decode()

def files(repo):
    out = subprocess.run(["git", "-C", str(WS / repo), "ls-tree", "-r", "--name-only", ref(repo)], capture_output=True, text=True).stdout.split()
    return [f for f in out if not (set(pathlib.PurePath(f).parts) & SKIP_PARTS) and pathlib.PurePath(f).name not in SKIP_FILES and not f.endswith(".jar")]

def main():
    rows, unassessed = [], []
    for repo_dir in sorted(p for p in WS.iterdir() if (p / ".git").exists()):
        repo = repo_dir.name
        for f in files(repo):
            try:
                lines = read(repo, f).splitlines()
            except UnicodeDecodeError:
                continue
            for n, line in enumerate(lines, 1):
                kinds = [k for k, rx in RX if rx.search(line)]
                if not kinds:
                    continue
                kind = kinds[0]
                if "/test" in f or f.startswith("test") or "Test" in f or f.endswith("smoke.sql"):
                    kind = "test"
                elif f.endswith((".md", ".txt")):
                    kind = "doc"
                elif f.endswith(".sql"):
                    kind = "SQL"
                elif f.endswith((".yml", ".yaml", ".json")) and kind == "hard-coded lag":
                    kind = "config"
                key = (repo, f)
                if key in A:
                    tranche, why, owner = A[key]
                elif key in NO_CHANGE:
                    tranche, why, owner = "none", NO_CHANGE[key], ""
                else:
                    tranche, why, owner = "?", "UNASSESSED", ""
                    unassessed.append(f"{repo}/{f}:{n}: {line.strip()[:120]}")
                rows.append(dict(repo=repo, path=f, line=n, snippet=line.strip()[:160], kind=kind, owners=owners_for(repo_dir, f),
                                 tranche=tranche, assessment=why, decision_owner=owner))
    if unassessed:
        print("\n".join(unassessed)); sys.exit(1)
    fileset = {(r["repo"], r["path"]) for r in rows}
    by_t = {t: len({(r["repo"], r["path"]) for r in rows if r["tranche"] == t}) for t in ("1", "2", "3", "none")}
    summary = dict(files=len(fileset), citations=len(rows), repos_affected=len({r["repo"] for r in rows}), repos_scanned=sum(1 for p in WS.iterdir() if (p / ".git").exists()), by_tranche=by_t,
                   decisions=sorted({r["decision_owner"] for r in rows if r["decision_owner"]}))
    out = pathlib.Path.home() / "t1work"
    (out / "inventory.json").write_text(json.dumps(dict(summary=summary, rows=rows), indent=2))
    md = ["# SETTLE-4471 T+1 inventory (programme T1-2027)", "",
          "Scope as given: EU, UK and CH regular-way trades, T+2 -> T+1, go-live **2027-10-11**. Estate: Arden, 20 repos in `~/finserv-workspace` (seeded by `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh`).", "",
          f"**{summary['files']} files, {summary['citations']} line citations, {summary['repos_affected']} of {summary['repos_scanned']} repos.** "
          f"Files by tranche: T1 {by_t['1']} · T2 {by_t['2']} · T3 (business decision) {by_t['3']} · no change {by_t['none']}. Unassessed: 0.", "",
          "Tranche 1 = instruction-generation path (core-dates, settlement-instruction-service, iso20022-fix-messages). Tranche 2 = downstream consumers, schedules, wording. Tranche 3 = needs a named business decision first.", "",
          "Repos scanned with no settlement-cycle hit: " + ", ".join(sorted({p.name for p in WS.iterdir() if (p / '.git').exists()} - {r['repo'] for r in rows})) + ".", "",
          "Method: regex scan over every tracked file (patterns in `inventory.py`: `T_PLUS_2`, `T+2`, lag constants, `plusDays(2)`, `DATEADD(day, 2`, tag 63 = 3, `default_cycle`, cut-off/scheduler times keyed to T+1, FX spot, recall notice, ex-date rules, two-arg `SettlementDates.settlementDate` callers that inherit the market default, test pins of T+2 dates). Owners = last matching CODEOWNERS rule.", ""]
    md += ["## Files", "", "| repo | file | kind | lines | CODEOWNERS | tranche | assessment | decision owner |", "|---|---|---|---|---|---|---|---|"]
    for repo, f in sorted(fileset):
        rs = [r for r in rows if (r["repo"], r["path"]) == (repo, f)]
        r0 = rs[0]
        md.append(f"| {repo} | `{f}` | {', '.join(sorted({r['kind'] for r in rs}))} | {', '.join(str(r['line']) for r in rs)} | {' '.join(r0['owners'])} | {r0['tranche']} | {r0['assessment']} | {r0['decision_owner'] or '-'} |")
    md += ["", "## Citations", "", "| # | repo | path:line | kind | snippet | CODEOWNERS | tranche |", "|---|---|---|---|---|---|---|"]
    for i, r in enumerate(rows, 1):
        snip = r["snippet"].replace("|", "\\|").replace("`", "'")
        md.append(f"| {i} | {r['repo']} | `{r['path']}:{r['line']}` | {r['kind']} | `{snip}` | {' '.join(r['owners'])} | {r['tranche']} |")
    (out / "reports/demo2-t1-inventory.md").write_text("\n".join(md) + "\n")
    print(json.dumps(summary, indent=1))

main()
