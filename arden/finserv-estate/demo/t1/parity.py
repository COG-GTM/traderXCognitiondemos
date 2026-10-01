#!/usr/bin/env python3
"""Demo 2, step 3 — golden-file parity: replay the same settlement-instruction input through the pre-migration and
post-migration core-dates CLI, list every difference, and classify each one as expected, unchanged, or ESCALATE.

    python3 demo/t1/parity.py     # -> demo/out/t1/parity.json, demo/reports/demo2-parity.html, demo/reports/demo2-migration-report.md

The harness never decides business questions.  A trade whose new settlement date falls on a day where the venue
calendar and the TARGET2 cash calendar disagree is reported with both candidate dates and routed to market-ops;
dividend events straddling go-live are listed with the two possible ex-dates and routed to asset-servicing (CA-2291).
"""
from __future__ import annotations

import csv
import datetime as dt
import io
import pathlib
import sys

import yaml

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, REPORTS, WORKSPACE, esc, html_page, java_env, log, now_iso, read_json, run, write_json  # noqa: E402

HERE = pathlib.Path(__file__).resolve().parent
GO_LIVE = dt.date(2027, 10, 11)
NORTH_AMERICA = {"XNYS", "XNAS", "XTSE"}
CASH_CALENDAR = {"XLON": "XLON", "XPAR": "TARGET2", "XETR": "TARGET2", "XSWX": "XSWX"}  # cash leg: CREST/GBP, T2/EUR, SIC/CHF
CALENDAR_DECISION = "@ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md)"
CA_DECISION = "asset-servicing ops, CA-2291 (corporate-actions-service/docs/ex-date-policy.md)"


def load_calendars() -> dict[str, set[dt.date]]:
    y = yaml.safe_load((WORKSPACE / "core-dates/src/main/resources/holidays.yaml").read_text())
    return {k: {dt.date.fromisoformat(d) for d in v} for k, v in y["calendars"].items()}


def add_business_days(d: dt.date, n: int, hol: set[dt.date]) -> dt.date:
    step = 1 if n >= 0 else -1
    while n != 0:
        d += dt.timedelta(days=step)
        if d.weekday() < 5 and d not in hol:
            n -= step
    return d


def is_business_day(d: dt.date, hol: set[dt.date]) -> bool:
    return d.weekday() < 5 and d not in hol


def run_cli(jar: pathlib.Path, golden: pathlib.Path) -> dict[str, dict]:
    r = run([java_env()["JAVA_HOME"] + "/bin/java", "-jar", str(jar)], HERE, env=java_env(), stdin=golden.read_text())
    if not r.ok:
        raise SystemExit(f"{jar.name} failed: {r.stdout[:300]}")
    return {row["trade_id"]: row for row in csv.DictReader(io.StringIO(r.stdout))}


def classify(trade: dict, old: dict, new: dict, cals: dict[str, set[dt.date]]) -> dict:
    td = dt.date.fromisoformat(trade["trade_date"])
    mic = trade["mic"]
    diffs = {k: (old[k], new[k]) for k in ("settlement_date", "tag64", "tag63", "affirmation_deadline") if old[k] != new[k]}
    row = {"trade_id": trade["trade_id"], "trade_date": trade["trade_date"], "mic": mic, "old": old, "new": new, "diffs": diffs}
    if not diffs:
        if mic in NORTH_AMERICA:
            row.update(verdict="unchanged", why="already T+1 (since 28 May 2024)")
        elif td < GO_LIVE:
            row.update(verdict="unchanged", why="trade date before go-live: T+2 preserved (date gate)")
        else:
            row.update(verdict="ESCALATE", why="post-go-live EU/UK/CH trade did not change — investigate")
        return row
    if td < GO_LIVE or mic in NORTH_AMERICA:
        row.update(verdict="ESCALATE", why="changed although out of scope of the date gate — regression")
        return row
    venue_hol = cals[mic]
    cash_hol = cals[CASH_CALENDAR[mic]]
    t = td if is_business_day(td, venue_hol) else add_business_days(td, 1, venue_hol)
    venue_sd = add_business_days(t, 1, venue_hol)
    cash_sd = add_business_days(t, 1, cash_hol)
    new_sd = dt.date.fromisoformat(new["settlement_date"])
    if new_sd != venue_sd:
        row.update(verdict="ESCALATE", why=f"new date {new_sd} != venue-calendar T+1 {venue_sd}")
        return row
    if venue_sd != cash_sd or not is_business_day(venue_sd, cash_hol):
        row.update(verdict="ESCALATE", why=f"venue ({mic}) settles {venue_sd} but cash calendar ({CASH_CALENDAR[mic]}) says {cash_sd} — "
                                          f"which is authoritative is a business decision", decision=CALENDAR_DECISION,
                   candidates={"venue": venue_sd.isoformat(), "cash": cash_sd.isoformat()})
        return row
    notes = []
    if td == GO_LIVE:
        notes.append("double-settlement day: settles alongside the last T+2 trades of 8 Oct")
    if new["affirmation_deadline"] == trade["trade_date"]:
        notes.append("affirmation deadline collapses onto trade date (tranche 2: cut-offs)")
    row.update(verdict="expected", why="T+2 → T+1; tag 63 3→2; tag 64 format unchanged" + (" · " + "; ".join(notes) if notes else ""))
    return row


def dividend_rows(cals: dict[str, set[dt.date]]) -> list[dict]:
    rows = []
    for e in csv.DictReader((HERE / "golden/dividend-events-2027Q4.csv").open()):
        rec = dt.date.fromisoformat(e["record_date"])
        ann = dt.date.fromisoformat(e["announced"])
        hol = cals[e["primary_mic"]]
        ex_t2 = add_business_days(rec, -1, hol)
        ex_t1 = rec
        row = {"event_id": e["event_id"], "isin": e["isin"], "mic": e["primary_mic"], "also_listed": e["also_listed"], "announced": e["announced"],
               "record_date": e["record_date"], "ex_date_t2_rule": ex_t2.isoformat(), "ex_date_t1_rule": ex_t1.isoformat()}
        if rec < GO_LIVE:
            row.update(verdict="unchanged", why="record date before go-live: ex-date = record − 1 business day")
        elif ann < GO_LIVE <= rec:
            row.update(verdict="ESCALATE", why="announced before go-live, record date after: policy does not say which ex-date rule applies", decision=CA_DECISION)
        elif e["also_listed"]:
            row.update(verdict="ESCALATE", why=f"dual-listed {e['primary_mic']}/{e['also_listed']}: ex-date must be identical across venues; not covered by policy", decision=CA_DECISION)
        else:
            row.update(verdict="pending-policy", why="post-go-live event: ex-date = record date once CA-2291 is ruled; ExDateCalculator not changed in tranche 1", decision=CA_DECISION)
        if row["verdict"] != "ESCALATE" and e["also_listed"] and rec >= GO_LIVE:
            row.update(verdict="ESCALATE", why="dual-listed security straddling go-live", decision=CA_DECISION)
        rows.append(row)
    return rows


def pill(v: str) -> str:
    cls = {"expected": "good", "unchanged": "info", "ESCALATE": "warn", "pending-policy": "info"}.get(v, "bad")
    return f'<span class="pill {cls}">{esc(v)}</span>'


def render_html(summary: dict, rows: list[dict], divs: list[dict], migration: dict) -> str:
    cards = "".join(f'<div class="card"><div class="k">{k}</div><div class="v">{v}</div></div>' for k, v in [
        ("golden trades", summary["trades"]), ("changed", summary["changed"]), ("expected", summary["expected"]),
        ("unchanged", summary["unchanged"]), ("escalated", summary["escalated"]), ("dividend events escalated", summary["dividends_escalated"])])
    trs = []
    for r in rows:
        def cell(k: str) -> str:
            o, n = r["old"][k], r["new"][k]
            return f'<td class="mono">{esc(o)}</td><td class="mono">{"<b>" + esc(n) + "</b>" if o != n else esc(n)}</td>'
        why = esc(r["why"]) + (f'<br><span class="mono" style="color:var(--warn)">→ {esc(r["decision"])}</span>' if r.get("decision") else "")
        trs.append(f'<tr><td class="mono">{r["trade_id"]}</td><td class="mono">{r["trade_date"]}</td><td>{r["mic"]}</td>'
                   f'{cell("settlement_date")}{cell("tag63")}{cell("affirmation_deadline")}<td>{pill(r["verdict"])}</td><td>{why}</td></tr>')
    dtrs = "".join(f'<tr><td class="mono">{d["event_id"]}</td><td class="mono">{d["isin"]}</td><td>{d["mic"]}{" / " + d["also_listed"] if d["also_listed"] else ""}</td>'
                   f'<td class="mono">{d["announced"]}</td><td class="mono">{d["record_date"]}</td><td class="mono">{d["ex_date_t2_rule"]}</td><td class="mono">{d["ex_date_t1_rule"]}</td>'
                   f'<td>{pill(d["verdict"])}</td><td>{esc(d["why"])}{"<br><span class=mono style=color:var(--warn)>→ " + esc(d["decision"]) + "</span>" if d.get("decision") else ""}</td></tr>'
                   for d in divs)
    mig = "".join(f'<tr><td>{m["repo"]}</td><td class="mono">{m["branch"]}</td><td>{pill("expected" if m["status"] == "migrated" else ("ESCALATE" if m["status"] == "blocked" else "pending-policy")).replace(">expected<", ">migrated<").replace(">pending-policy<", ">deviation<")}</td>'
                  f'<td>{"<br>".join(esc(("PASS " if a["tests_ok"] else "FAIL ") + a["strategy"]) for a in m["attempts"])}</td><td>{esc(", ".join(m["owners"]))}</td></tr>' for m in migration["results"])
    body = f"""
<h1>T+1 golden-file parity — SETTLE-4471</h1>
<p class="sub">EU/UK/CH regular way T+2 → T+1, go-live 11 Oct 2027. Same 30-trade golden file replayed through core-dates {summary['baseline']} (production pin)
and {summary['migrated']} (tranche 1). Every difference is listed; nothing marked ESCALATE was resolved in code. Generated {summary['generated_at']}.</p>
<div class="grid">{cards}</div>
<h2>Tranche 1 execution</h2>
<table><tr><th>repo</th><th>branch</th><th>status</th><th>steps</th><th>CODEOWNERS</th></tr>{mig}</table>
<h2>Old vs new — settlement instructions</h2>
<table><tr><th>trade</th><th>trade date</th><th>MIC</th><th>settle (old)</th><th>settle (new)</th><th>tag 63 old</th><th>new</th><th>affirm by (old)</th><th>new</th><th>verdict</th><th>why</th></tr>
{''.join(trs)}</table>
<h2>Corporate actions — ex-date under both regimes</h2>
<p class="sub">Not executed in tranche 1. Listed so the business owner sees exactly which events need a ruling before ExDateCalculator changes.</p>
<table><tr><th>event</th><th>ISIN</th><th>venue(s)</th><th>announced</th><th>record</th><th>ex-date (T+2 rule)</th><th>ex-date (T+1 rule)</th><th>verdict</th><th>why</th></tr>{dtrs}</table>
"""
    return html_page("T+1 parity — SETTLE-4471", body, "Arden Capital Markets (synthetic estate) · demo/t1/parity.py · human decisions are routed, not guessed")


def render_md(summary: dict, rows: list[dict], divs: list[dict], migration: dict, inventory: dict) -> str:
    inv = inventory["summary"]
    out = ["# T+1 migration — tranche 1 report (SETTLE-4471)", "",
           f"Generated {summary['generated_at']}. Scope: EU, UK, Switzerland regular-way settlement T+2 → T+1 on **11 October 2027**. "
           "Synthetic estate (Arden Capital Markets, 20 repos).", "",
           "## 1. What was found", "",
           f"- {inv['files']} files in {inv['repos_affected']} repositories carry a settlement-cycle assumption ({inv['citations']} line citations, `demo2-t1-inventory.md`).",
           f"- Tranche split: {inv['by_tranche']['1']} files in the instruction path (tranche 1), {inv['by_tranche']['2']} downstream/scheduler/wording (tranche 2), "
           f"{inv['by_tranche']['3']} that need a business decision first (tranche 3).", f"- Decisions owed: {', '.join(inv['decisions'])}.", "",
           "## 2. What was changed (tranche 1)", ""]
    for m in migration["results"]:
        out.append(f"### {m['repo']} — `{m['status']}` · branch `{m['branch']}` · reviewers {', '.join(m['owners'])}")
        for i, a in enumerate(m["attempts"], 1):
            out.append(f"{i}. {'PASS' if a['tests_ok'] else 'FAIL'} — {a['strategy']}  \n   `{a['test_summary']}`")
            if a.get("reason"):
                out.append(f"   - {a['reason']}")
        out.append(f"   PR artifact: `{pathlib.Path(m['pr_dir']).relative_to(OUT.parents[1])}/PR.md`")
        out.append("")
    out += ["## 3. Parity — what the counterparty would see", "",
            f"Golden file `demo/t1/golden/trades-2027Q4.csv` ({summary['trades']} trades) through core-dates {summary['baseline']} vs {summary['migrated']}: "
            f"**{summary['changed']} changed**, {summary['unchanged']} unchanged, {summary['expected']} expected, **{summary['escalated']} escalated**.", "",
            "| trade | date | MIC | settle old → new | tag 63 | affirm-by old → new | verdict | why |", "|---|---|---|---|---|---|---|---|"]
    for r in rows:
        o, n = r["old"], r["new"]
        arrow = lambda k: f"{o[k]}" if o[k] == n[k] else f"{o[k]} → **{n[k]}**"  # noqa: E731
        out.append(f"| {r['trade_id']} | {r['trade_date']} | {r['mic']} | {arrow('settlement_date')} | {arrow('tag63')} | {arrow('affirmation_deadline')} | {r['verdict']} | {r['why']}{' → ' + r['decision'] if r.get('decision') else ''} |")
    out += ["", "## 4. Escalations — decisions the code does not make", ""]
    for r in [x for x in rows if x["verdict"] == "ESCALATE"]:
        c = r.get("candidates", {})
        out.append(f"- **{r['trade_id']}** ({r['trade_date']} {r['mic']}): {r['why']}." + (f" Candidates: venue {c['venue']} / cash {c['cash']}." if c else "") + f" Owner: {r.get('decision', 'engineering')}")
    for d in [x for x in divs if x["verdict"] == "ESCALATE"]:
        out.append(f"- **{d['event_id']}** ({d['isin']}, record {d['record_date']}): {d['why']}. Ex-date would be {d['ex_date_t2_rule']} (T+2 rule) or {d['ex_date_t1_rule']} (T+1 rule). Owner: {d['decision']}")
    out += ["", "## 5. Not done, on purpose", "",
            "- Tranche 2 (schedulers, cut-offs, recon, risk, client wording, legacy SQL) is planned per repo in `demo2-t1-migration-plan.md`, not executed.",
            "- Tranche 3 items (calendar authority, ex-date regime, legacy position keeper, FX funding, GMSLA recall notice) are escalated above and left unchanged.",
            "- No branch is merged; no BOM pin moved. `core-dates` main still carries the 1.5.0 wire change and needs its own decision before tranche 1 can be forward-ported.", ""]
    return "\n".join(out)


def main() -> None:
    golden = HERE / "golden/trades-2027Q4.csv"
    trades = list(csv.DictReader(golden.open()))
    cals = load_calendars()
    old = run_cli(OUT / "t1/baseline-cli.jar", golden)
    new = run_cli(OUT / "t1/migrated-cli.jar", golden)
    rows = [classify(t, old[t["trade_id"]], new[t["trade_id"]], cals) for t in trades]
    divs = dividend_rows(cals)
    migration = read_json(OUT / "t1/migration-results.json")
    inventory = read_json(OUT / "t1/inventory.json")
    bv = migration["results"][0]["attempts"][0]["test_summary"].rsplit(" ", 1)[-1]
    mv = next((a["strategy"].split("core-dates ")[-1].split(" ")[0] for a in reversed(migration["results"][0]["attempts"]) if a["tests_ok"]), "?")
    summary = {"generated_at": now_iso(), "go_live": GO_LIVE.isoformat(), "baseline": bv, "migrated": mv, "trades": len(rows),
               "changed": sum(bool(r["diffs"]) for r in rows), "expected": sum(r["verdict"] == "expected" for r in rows),
               "unchanged": sum(r["verdict"] == "unchanged" for r in rows), "escalated": sum(r["verdict"] == "ESCALATE" for r in rows),
               "dividends_escalated": sum(d["verdict"] == "ESCALATE" for d in divs)}
    write_json(OUT / "t1/parity.json", {"summary": summary, "trades": rows, "dividends": divs})
    REPORTS.mkdir(parents=True, exist_ok=True)
    html = render_html(summary, rows, divs, migration)
    (OUT / "t1/parity.html").write_text(html)
    (REPORTS / "demo2-parity.html").write_text(html)
    (REPORTS / "demo2-migration-report.md").write_text(render_md(summary, rows, divs, migration, inventory))
    log(f"parity: {summary['trades']} trades, {summary['changed']} changed, {summary['expected']} expected, {summary['unchanged']} unchanged, "
        f"{summary['escalated']} ESCALATED (+{summary['dividends_escalated']} dividend events) -> demo/reports/demo2-parity.html")
    for r in rows:
        if r["verdict"] == "ESCALATE":
            log(f"  ESCALATE {r['trade_id']} {r['trade_date']} {r['mic']}: {r['why']}")


if __name__ == "__main__":
    main()
