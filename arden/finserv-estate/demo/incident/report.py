#!/usr/bin/env python3
"""Demo 3, step 3: leave-behinds: a timeline page, the incident record and the RCA.

    python3 demo/incident/report.py

Outputs
  demo/out/incident/slack-thread.md            dry-run thread artifact (not a presenter step)
  demo/reports/demo3-incident-timeline.html    alert → leads → root cause → fix, on one time axis
  demo/reports/demo3-incident-record.md        DORA/ICT-incident-style record; classification left to the human
  demo/reports/demo3-rca.md                    root cause, why the lead was wrong, what would have caught it
"""
from __future__ import annotations

import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, REPORTS, esc, html_page, log, read_json, write_json  # noqa: E402


def slack_messages(inp: dict, inv: dict) -> list[dict]:
    a, s = inp["alert"], {x["id"]: x for x in inv["steps"]}
    lead, root, fix, gaps = s[2], s[3], s[4], s[5]
    headline = a["body"].strip("%\n ").splitlines()[0]
    msgs = [
        {"who": "Datadog", "at": a["fired_at"], "text": f":rotating_light: *{a['title']}*\n{headline}\n<{a['link']}|Monitor {a['monitor_id']}> · {a['priority']}"},
        {"who": "Devin", "at": s[1]["at"], "text": f"Reproduced in 1 step: `{inp['errors'][0]['message']}` at `src/valuation/valuation.js`, deterministic with the live ArdenFeed batch. "
                                                          f"First error {inv['first_error']}, last healthy {inv['last_healthy']}. Checking the obvious lead first."},
        {"who": "Devin", "at": lead["at"], "text": f":mag: *Lead A: {lead['title']}*\n" + "\n".join(f"• {e}" for e in lead["evidence"][:4]) + f"\n*{lead['verdict']}*"},
        {"who": "Devin", "at": root["at"], "text": f":dart: *Lead B: the input changed*\n" + "\n".join(f"• {e}" for e in root["evidence"][:4]) + f"\n*{root['verdict']}*"},
        {"who": "Devin", "at": fix["at"], "text": f":wrench: *Fix on branch `{inv['branch']}`* (not deployed, @ardencm/client-digital to decide)\n" + "\n".join(f"• {e}" for e in fix["evidence"][:5])
                                                           + f"\nPR body + patch: `{pathlib.Path(inv['pr_dir']).relative_to(OUT.parents[1])}/`"},
        {"who": "Devin", "at": gaps["at"], "text": ":memo: *Not fixed here: for humans*\n" + "\n".join(f"• {e}" for e in gaps["evidence"]) + f"\nIncident record + RCA drafted: `demo/reports/demo3-incident-record.md`, `demo/reports/demo3-rca.md`. "
                                                           "Regulatory classification (DORA major-incident test) is *not* made here: ops risk to decide."},
    ]
    return msgs


def timeline_html(inp: dict, inv: dict) -> str:
    a = inp["alert"]
    steps = {s["id"]: s for s in inv["steps"]}
    before = [
        ("11 Sep 2026", "Vendor change notice received; ArdenFeed schema 2.3 effective 25 Sep 06:00, no parallel run."),
        ("22 Sep 2026", "eod-pricing-batch moved to the schema 2.3 parser for end-of-day pricing."),
        ("24 Sep 2026, 21:05 UTC", f"client-portal {inv['latest_deploy']['version']} deployed: {inv['latest_deploy']['summary']}"),
    ]
    events = [
        (inv["last_healthy"], "ok", "last healthy /api/portfolio/valuation (feed schema 2.2)"),
        ("2026-09-25T06:00:00Z", "vendor", "ArdenFeed v2.3 goes live: px becomes { v, ccy }"),
        (inv["first_error"], "error", f"first 500: {inp['errors'][0]['message']}"),
        (a["fired_at"], "alert", f"Datadog monitor {a['monitor_id']} alerts; investigation starts"),
    ] + [(s["at"], "step", f"{s['title']}: {s['verdict']}") for s in inv["steps"]]
    events.sort(key=lambda event: event[0])
    cls = {"vendor": "info", "deploy": "warn", "ok": "good", "error": "bad", "alert": "bad", "step": "info"}
    items = "".join(f'<div class="tl"><div class="t mono">{esc(t)}</div><div class="dot {cls[k]}"></div><div class="d">{esc(txt)}</div></div>' for t, k, txt in events)
    cards = "".join(f'<div class="card"><div class="k">{k}</div><div class="v">{esc(v)}</div></div>' for k, v in [
        ("detect to alert", "4 min"), ("leads tested", "2"), ("ruled out", "1 (latest deploy)"),
        ("root cause", "ArdenFeed schema 2.3"), ("fix", inv["status"]), ("deployed", "no, human decision")])
    context = "".join(f'<li><b>{esc(date)}</b>: {esc(text)}</li>' for date, text in before)
    verdict = (f"Deploy {esc(inv['latest_deploy']['version'])}: "
               f"{'INNOCENT' if steps[2]['ruled_out'] else 'NOT RULED OUT'} "
               "(pre-deploy release fails identically on the live batch; a rollback would have kept the portal down)")
    root_cause = "ArdenFeed schema 2.3 (px number -> { v, ccy })"
    body = f"""
<h1>{esc(inv['incident'])}: client-portal valuation 5xx</h1>
<p class="sub"><b>{esc(a['title'])}</b>. Investigation evidence, in time order, on 25 September 2026.</p>
<section class="verdict"><div>{verdict}</div><strong>Root cause: {root_cause}</strong></section>
<div class="grid">{cards}</div>
<style>.tl{{display:grid;grid-template-columns:190px 18px 1fr;gap:12px;align-items:start;padding:8px 0;border-bottom:1px solid rgba(255,255,255,.06)}}
.dot{{width:12px;height:12px;border-radius:50%;margin-top:5px;box-shadow:0 0 12px currentColor}}.dot.good{{background:var(--good);color:var(--good)}}
.dot.bad{{background:var(--bad);color:var(--bad)}}.dot.warn{{background:var(--warn);color:var(--warn)}}.dot.info{{background:var(--accent);color:var(--accent)}}
 .d{{color:var(--fg)}}.t{{color:var(--muted);font-size:13px}}
.verdict{{border:2px solid var(--bad);border-radius:14px;padding:20px 24px;margin:24px 0;background:rgba(248,113,113,.08);font-size:19px;line-height:1.55}}
.verdict strong{{display:block;color:#fca5a5;margin-top:10px;font-size:22px}}
.context{{border:1px solid var(--line);border-radius:14px;padding:18px 24px;background:var(--panel)}}.context li{{margin:7px 0}}</style>
<h2>Before the incident</h2><ul class="context">{context}</ul>
<h2 id="timeline">Timeline (UTC)</h2>{items}
<h2>Evidence per step</h2>
{''.join(f'<h3>{s["id"]}. {esc(s["title"])}</h3><ul>' + ''.join(f'<li>{esc(e)}</li>' for e in s["evidence"]) + f'</ul><p><b>{esc(s["verdict"])}</b></p>' for s in inv["steps"])}
"""
    return html_page(f"{inv['incident']} timeline", body, "Arden Capital Markets (synthetic estate) · demo/incident/report.py · rollout and regulatory classification are human decisions")


def incident_record(inp: dict, inv: dict) -> str:
    a = inp["alert"]
    return f"""# Incident record: {inv['incident']} (DRAFT, for ops-risk review)

| Field | Value | Source |
|---|---|---|
| Service | client-portal (institutional client portal, intraday valuations) | monitor scope `{a['scope']}` |
| Detected | {a['fired_at']} by Datadog monitor {a['monitor_id']} (`valuation 5xx rate > 5%`) | Datadog webhook |
| Impact start | {inv['first_error']} (first 500 on `/api/portfolio/valuation`) | Datadog logs |
| Last known good | {inv['last_healthy']} | Datadog logs |
| Impact | 100% of intraday valuation requests failing; positions/settlement routes unaffected | monitor body, logs |
| Root cause | {inv['root_cause']} | investigation step 3 |
| Contributing | vendor change notice (11 Sep) actioned for eod-pricing-batch only; consumer register listed client-portal on 2.2; no parallel run offered | market-data-feed-contracts |
| False lead | {inv['false_lead']}: ruled out by replaying the live batch on the pre-deploy release | investigation step 2 |
| Fix | branch `{inv['branch']}`: normaliser accepts 2.2/2.3, rejects unknown shapes; 4/4 tests, lint ok | investigation step 4 |
| Deployed | **not yet**: @ardencm/client-digital + release-bot |: |
| Data integrity | no valuations were served wrong; requests failed closed (500) | logs: no 200 with schema 2.3 before the fix |
| Client impact | to be confirmed by client-digital (portal sessions 06:00–resolution) |: |
| Regulatory classification | **not assessed here.** DORA Art. 18 major-incident criteria (clients affected, duration, geographic spread, data losses, criticality, economic impact) need ops-risk input; if major, initial notification is due 4h after classification / 24h after awareness | human |
| Owners | service: @ardencm/client-digital · feed: @ardencm/market-data · vendor: vendor management | CODEOWNERS |

Investigation finished {inv['finished_at']}. This record is a draft produced by the investigation; the classification, client-impact and closure fields are owned by humans.
"""


def rca(inp: dict, inv: dict) -> str:
    s = {x["id"]: x for x in inv["steps"]}
    ev = lambda i: "\n".join(f"- {e}" for e in s[i]["evidence"])  # noqa: E731
    return f"""# RCA: {inv['incident']}: client-portal valuation failures after ArdenFeed v2.3 cutover (DRAFT)

## Summary
At 06:00 UTC on 25 Sep 2026 ArdenFeed switched its quote schema from 2.2 to 2.3 (`px` number → `{{ v, ccy }}` object) with no parallel run.
client-portal's quote normaliser still assumed 2.2, so every intraday valuation request failed with `TypeError: n.px.toFixed is not a function`.
Datadog paged at {inp['alert']['fired_at']}. The most recent deploy (31.4.2, memoisation of the same function, 8.9h earlier) looked responsible and was not.

## What happened
{ev(1)}

## The lead that was wrong, and how it was ruled out
{ev(2)}

**{s[2]['verdict']}.** A rollback: the standard first move: would have kept the portal down.

## Root cause
{ev(3)}

**{s[3]['verdict']}.**

## Fix
{ev(4)}

Rollout is a human decision. The fix fails loudly on any *third* shape rather than silently passing objects through.

## Why this was not caught earlier
{ev(5)}

## Recommendations (owners decide; nothing below is done)
1. market-data: route vendor notices to every consumer in `consumers.yaml`, not to the ticket's scope. Owner @ardencm/market-data.
2. market-data-feed-contracts: add a consumer contract test per registered consumer that runs the consumer's parser against each schema sample. Owner @ardencm/market-data + consumers.
3. client-portal: monitor on parse failures / quote schema version, alerting before the 5xx rate does. Owner @ardencm/client-digital.
4. Vendor management: require a parallel-run window in the ArdenFeed contract. Owner vendor management.

## What this RCA does not decide
Regulatory classification, client communications and whether 31.4.2's memoisation should stay are outside the investigation's remit.

Investigation finished {inv['finished_at']}.
"""


def main() -> None:
    inp = read_json(OUT / "incident/incident-input.json")
    inv = read_json(OUT / "incident/investigation.json")
    msgs = slack_messages(inp, inv)
    write_json(OUT / "incident/slack-thread.json", msgs)
    (OUT / "incident/slack-thread.md").write_text(f"# #inc-client-portal: {inv['incident']}\n\n" + "\n\n---\n\n".join(f"**{m['who']}** · `{m['at']}`\n\n{m['text']}" for m in msgs) + "\n")
    REPORTS.mkdir(parents=True, exist_ok=True)
    html = timeline_html(inp, inv)
    (OUT / "incident/timeline.html").write_text(html)
    (REPORTS / "demo3-incident-timeline.html").write_text(html)
    (REPORTS / "demo3-incident-record.md").write_text(incident_record(inp, inv))
    (REPORTS / "demo3-rca.md").write_text(rca(inp, inv))
    log("reports: demo/reports/demo3-incident-timeline.html, demo3-incident-record.md, demo3-rca.md")


if __name__ == "__main__":
    main()
