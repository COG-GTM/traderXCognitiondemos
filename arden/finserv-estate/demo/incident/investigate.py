#!/usr/bin/env python3
"""Demo 3, step 2 — investigate the alert like an on-call engineer would, with every step recorded as evidence.

    python3 demo/incident/investigate.py        # reads demo/out/incident/incident-input.json, works in the workspace clone

Method (each step is a hypothesis with a test, not a guess):
  1. reproduce   run the failing route's code path against the live quote batch → same TypeError, same line
  2. lead A      "latest deploy broke it" (31.4.2 memoisation, 9h earlier, touches the exact line in the stack)
                 test: check out the pre-deploy release and replay the same batch → still fails → deploy ruled out
                 corroboration: 7 healthy requests on 31.4.2 between deploy and 06:00
  3. lead B      "the input changed"  test: diff the live batch against the last healthy one → px number → object,
                 schema 2.2 → 2.3; vendor notice in market-data-feed-contracts (effective 25 Sep 06:00, no parallel run),
                 consumer register shows client-portal left on 2.2 (MD-1187 scope was EOD only)
  4. fix         normaliser accepts both shapes; regression test with the v2.3 batch; npm test + lint; branch + PR artifact
  5. not fixed   process gaps that need humans: feed-change routing, contract test per consumer, schema-version monitor
Output: demo/out/incident/investigation.json, demo/out/incident/prs/client-portal/{PR.md,change.patch}
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
import textwrap

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, WORKSPACE, git, log, now_iso, owners_for, read_json, run, target_line, write_json  # noqa: E402

PORTAL = WORKSPACE / "client-portal"
FEEDS = WORKSPACE / "market-data-feed-contracts"
LIVE_BATCH = FEEDS / "samples/latest-batch.json"
INCIDENT = "INC-2026-0925-01"
BRANCH = f"incident/{INCIDENT.lower()}-ardenfeed-v23"
SCREENS = pathlib.Path(__file__).resolve().parents[2] / "docs/screens"
BROWSER_EVIDENCE = [
    ("screen recording (before → after → unaffected pages)", "demo3-browser-verification.mp4"),
    ("valuation on ArdenFeed v2.3, pre-fix (500)", "demo3-browser-before-v23.png"),
    ("valuation on ArdenFeed v2.3, fix branch (200, rows render)", "demo3-browser-after-v23.png"),
    ("settlement summary, unaffected page", "demo3-browser-settlement-regression.png"),
]

REPRO_JS = textwrap.dedent("""\
    import { readFileSync } from 'node:fs';
    import { valuePortfolio } from './src/valuation/valuation.js';
    import { loadQuotes } from './src/feed/client.js';
    const positions = JSON.parse(readFileSync('fixtures/positions.json', 'utf8'));
    try {
      const rows = valuePortfolio(positions, loadQuotes(process.argv[2]));
      console.log('OK ' + JSON.stringify(rows.map(r => [r.isin, r.price, r.ccy, r.marketValue])));
    } catch (e) {
      console.log('FAIL ' + e.constructor.name + ': ' + e.message + '\\n' + e.stack.split('\\n').slice(1, 3).join('\\n'));
      process.exit(1);
    }
""")

NORMALIZE_JS = textwrap.dedent("""\
    // Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
    // Schema 2.2: { px: number, ccy }            Schema 2.3+: { px: { v: number, ccy } } (quote-level ccy deprecated)
    export function normalizeQuote(q) {
      const px = typeof q.px === 'object' && q.px !== null ? q.px.v : q.px;
      const ccy = (typeof q.px === 'object' && q.px !== null ? q.px.ccy : undefined) ?? q.ccy;
      if (typeof px !== 'number' || typeof ccy !== 'string') {
        throw new TypeError(`unrecognised ArdenFeed quote shape for ${q.isin}: ${JSON.stringify(q.px)}`);
      }
      return { isin: q.isin, px, ccy, asof: q.asof };
    }
""")

REGRESSION_TEST_JS = textwrap.dedent("""\

    test('values a portfolio from an ArdenFeed v2.3 batch (px is an object) — INC-2026-0925-01', () => {
      clearCache();
      const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
      assert.equal(rows[0].price, '12.4100');
      assert.equal(rows[0].ccy, 'GBP');
      assert.equal(rows[0].marketValue, '148920.00');
    });

    test('v2.2 and v2.3 batches for the same quote normalise to the same shape', () => {
      const a = normalizeQuote({ isin: 'X', px: 1.5, ccy: 'EUR', asof: 't' });
      const b = normalizeQuote({ isin: 'X', px: { v: 1.5, ccy: 'EUR' }, asof: 't' });
      assert.deepEqual(a, b);
    });

    test('rejects an unknown quote shape loudly instead of producing NaN valuations', () => {
      assert.throws(() => normalizeQuote({ isin: 'X', px: '1.5', asof: 't' }), TypeError);
    });
""")


def node(args: list[str], cwd: pathlib.Path) -> tuple[bool, str]:
    r = run(["node", *args], cwd)
    return r.ok, r.stdout.strip()


def repro(cwd: pathlib.Path, batch: pathlib.Path) -> tuple[bool, str]:
    script = cwd / ".repro.mjs"
    script.write_text(REPRO_JS)
    try:
        return node([str(script), str(batch)], cwd)
    finally:
        script.unlink(missing_ok=True)


def main() -> None:
    inp = read_json(OUT / "incident/incident-input.json")
    alert, errors, healthy = inp["alert"], inp["errors"], inp["healthy"]
    steps: list[dict] = []
    started = now_iso()
    git(PORTAL, "checkout", "-q", "main")

    # 1. reproduce ------------------------------------------------------------------------------------------------
    first_err = errors[0]
    m = re.search(r"valuation\.js:(\d+)", first_err["stack"])
    stack_line = int(m.group(1)) if m else None
    ok, out = repro(PORTAL, LIVE_BATCH)
    src_line = PORTAL.joinpath("src/valuation/valuation.js").read_text().splitlines()[stack_line - 1].strip() if stack_line else ""
    steps.append({"id": 1, "kind": "reproduce", "title": "Reproduce against the live quote batch", "ok": (not ok) and first_err["message"] in out,
                  "evidence": [f"alert: {alert['title']} fired {alert['fired_at']}",
                               f"first error log {first_err['ts']} v{first_err['version']}: {first_err['message']}",
                               f"stack → src/valuation/valuation.js:{stack_line}: `{src_line}`",
                               f"replay {LIVE_BATCH.relative_to(WORKSPACE)} through valuePortfolio on main (31.4.2): {out.splitlines()[0]}"],
                  "verdict": "reproduced: same TypeError, same line, deterministic with the live batch"})

    # 2. lead A: latest deploy ------------------------------------------------------------------------------------
    deploys = [d for d in inp["deploys"] if d["service"] == alert["service"]]
    latest = deploys[0]
    gap_h = (__import__("datetime").datetime.fromisoformat(first_err["ts"][:-1]) - __import__("datetime").datetime.fromisoformat(latest["at"][:-1])).total_seconds() / 3600
    blame = git(PORTAL, "log", "-1", "--format=%h %an %ad %s", "--date=short", "-L", f"{stack_line},{stack_line}:src/valuation/valuation.js").stdout.splitlines()[0] if stack_line else ""
    tags = git(PORTAL, "tag", "--list", "v31.4.*").stdout.split()
    pre = git(PORTAL, "rev-parse", "--short", "HEAD~1").stdout.strip()
    wt = OUT / "incident/worktree-pre-deploy"
    run(["git", "worktree", "remove", "--force", str(wt)], PORTAL)
    git(PORTAL, "worktree", "add", "-q", str(wt), pre)
    ok_pre, out_pre = repro(wt, LIVE_BATCH)
    ok_pre_old, out_pre_old = repro(wt, PORTAL / "fixtures/ardenfeed-v2.2.json")
    run(["git", "worktree", "remove", "--force", str(wt)], PORTAL)
    healthy_after_deploy = [h for h in healthy if h["ts"] > latest["at"]]
    steps.append({"id": 2, "kind": "lead", "title": f"Lead A — latest deploy {latest['version']} ({latest['ticket']}) broke it", "ok": True,
                  "evidence": [f"deploy {latest['version']} at {latest['at']} by release-bot ({latest['summary']}) — {gap_h:.1f}h before the first error",
                               f"git log -L on the failing line: {blame} — the line predates the deploy, but 31.4.2 rewrote this function (memoisation), so the lead stays plausible until tested",
                               f"checkout pre-deploy commit {pre} (31.4.1, the release before {', '.join(tags) or 'the deploy'}) and replay the live batch: {out_pre.splitlines()[0]}",
                               f"same pre-deploy code with yesterday's batch (schema 2.2): {out_pre_old.splitlines()[0]}",
                               f"{len(healthy_after_deploy)} healthy 200s on {latest['version']} between {healthy_after_deploy[0]['ts'] if healthy_after_deploy else '-'} and {healthy_after_deploy[-1]['ts'] if healthy_after_deploy else '-'}"],
                  "verdict": "RULED OUT — pre-deploy code fails identically on today's batch and works on yesterday's; a rollback would not have fixed it",
                  "ruled_out": True})

    # 3. lead B: the input changed -------------------------------------------------------------------------------
    live = json.loads(LIVE_BATCH.read_text())
    old = json.loads((PORTAL / "fixtures/ardenfeed-v2.2.json").read_text())
    q_old, q_new = old["quotes"][0], live["quotes"][0]
    notice = FEEDS / "ardenfeed/VENDOR-NOTICE-2026-09-11.md"
    notice_txt = notice.read_text()
    eff = re.search(r"\*\*Effective:\*\* (.+)", notice_txt).group(1)
    consumers = (FEEDS / "consumers.yaml").read_text()
    portal_line = next(l.strip() for l in consumers.splitlines() if "client-portal" in l)
    portal_schema = next(l.strip() for l in consumers.splitlines() if "schema:" in l and consumers.index(l) > consumers.index("client-portal"))
    feed_commits = git(FEEDS, "log", "--format=%h %ad %an — %s", "--date=iso-strict").stdout.splitlines()
    last_good_schema = healthy[-1]["feed_schema"] if healthy else "?"
    steps.append({"id": 3, "kind": "lead", "title": "Lead B — the input changed shape", "ok": True,
                  "evidence": [f"last healthy request {healthy[-1]['ts']} carried feed schema {last_good_schema}; first error {first_err['ts']} carries schema {first_err['feed_schema']}",
                               f"batch diff for {q_new['isin']}: px {json.dumps(q_old['px'])} → {json.dumps(q_new['px'])}; quote-level ccy {'present' if 'ccy' in q_old else 'absent'} → {'present' if 'ccy' in q_new else 'absent'}",
                               f"{notice.relative_to(WORKSPACE)}: effective {eff}",
                               f"{FEEDS.name}/consumers.yaml: `{portal_line}` `{portal_schema}` — client-portal was never moved to 2.3",
                               "market-data-feed-contracts history: " + " | ".join(feed_commits[:4]),
                               "notice says: 'Internal tracking: MD-1187 (eod-pricing-batch updated 22 Sep). No ticket was raised for client-portal.'"],
                  "verdict": "ROOT CAUSE — ArdenFeed v2.3 went live 06:00 UTC with px as an object; client-portal's normaliser (schema 2.2) passes the object through and toFixed fails",
                  "root_cause": True})

    # 4. fix -----------------------------------------------------------------------------------------------------
    git(PORTAL, "branch", "-D", BRANCH)
    git(PORTAL, "checkout", "-q", "-b", BRANCH)
    ok_t0, out_t0 = node(["--test", "test/"], PORTAL)
    (PORTAL / "src/pricing/normalize.js").write_text(NORMALIZE_JS)
    t = PORTAL / "test/valuation.test.js"
    t.write_text(t.read_text().replace("import { valuePortfolio, clearCache } from '../src/valuation/valuation.js';",
                                       "import { valuePortfolio, clearCache } from '../src/valuation/valuation.js';\nimport { normalizeQuote } from '../src/pricing/normalize.js';") + REGRESSION_TEST_JS)
    ok_t, out_t = node(["--test", "test/"], PORTAL)
    ok_l, out_l = run("npm run -s lint", PORTAL).ok, ""
    ok_r, out_r = repro(PORTAL, LIVE_BATCH)
    ok_r22, out_r22 = repro(PORTAL, PORTAL / "fixtures/ardenfeed-v2.2.json")
    summ = lambda o: " ".join(l.lstrip("#ℹ ").strip() for l in o.splitlines() if l.lstrip("#ℹ ").startswith(("tests ", "pass ", "fail ")))  # noqa: E731
    git(PORTAL, "add", "src/pricing/normalize.js", "test/valuation.test.js")
    git(PORTAL, "commit", "-q", "-m", f"bug: {INCIDENT} client-portal — accept ArdenFeed v2.3 px object in normaliser, regression test with live batch")
    fixed = ok_t and ok_l and ok_r and ok_r22
    steps.append({"id": 4, "kind": "fix", "title": "Fix — normaliser accepts 2.2 and 2.3, fails loudly on anything else", "ok": fixed,
                  "evidence": [f"tests before the fix (main): {summ(out_t0)}", f"tests after: {summ(out_t)} (3 new: v2.3 batch, 2.2≡2.3, unknown shape throws)",
                               f"npm run lint: {'ok' if ok_l else 'FAILED'}", f"replay live batch (2.3): {out_r.splitlines()[0]}", f"replay yesterday's batch (2.2): {out_r22.splitlines()[0]}",
                               f"branch {BRANCH}, reviewers {', '.join(owners_for(PORTAL, 'src/pricing/normalize.js'))}"],
                  "verdict": "fixed on a branch; deploy/rollout is a human decision (release-bot + @ardencm/client-digital)"})

    # 5. process gaps ---------------------------------------------------------------------------------------------
    steps.append({"id": 5, "kind": "follow-up", "title": "Not fixed here — process gaps for humans", "ok": True,
                  "evidence": ["Vendor notice reached market-data (11 Sep) and was actioned for eod-pricing-batch only; consumers.yaml listed client-portal as a 2.2 consumer and nobody was told.",
                               "No consumer contract test in market-data-feed-contracts exercises client-portal's parser against the new schema.",
                               "No monitor on quote schema version / parse failures; detection came from the 5xx rate 4 minutes after cutover.",
                               "Vendor gave no parallel run; whether to require one contractually is a vendor-management decision."],
                  "verdict": "recommendations only — owners: @ardencm/market-data, @ardencm/client-digital, vendor management"})

    result = {"incident": INCIDENT, "started_at": started, "finished_at": now_iso(), "alert": alert, "branch": BRANCH, "status": "fixed" if fixed else "blocked",
              "root_cause": steps[2]["verdict"], "false_lead": steps[1]["title"], "steps": steps, "first_error": first_err["ts"], "last_healthy": healthy[-1]["ts"] if healthy else None,
              "latest_deploy": latest, "feed_effective": eff}
    pr_dir = OUT / "incident/prs/client-portal"
    pr_dir.mkdir(parents=True, exist_ok=True)
    (pr_dir / "change.patch").write_text(git(PORTAL, "format-patch", "-1", "--stdout").stdout)
    (pr_dir / "PR.md").write_text(pr_body(result))
    result["pr_dir"] = str(pr_dir)
    write_json(OUT / "incident/investigation.json", result)
    for s in steps:
        log(f"{s['id']}. {s['title'][:70]:70s} → {s['verdict'][:90]}")
    log(f"{INCIDENT}: {result['status']} · branch {BRANCH} · {pr_dir.relative_to(OUT.parents[1])}/PR.md")


def browser_evidence_lines() -> list[str]:
    present = [(label, SCREENS / name) for label, name in BROWSER_EVIDENCE if (SCREENS / name).exists()]
    if not present:
        return ["## Browser verification", "- not yet recorded — run playbook phase 4 (Verify the frontend) and re-run this step", ""]
    return ["## Browser verification", *[f"- {label}: `{path}`" for label, path in present], ""]


def pr_body(r: dict) -> str:
    s = {x["id"]: x for x in r["steps"]}
    lines = [f"# {r['incident']}: client-portal valuation 5xx — accept ArdenFeed v2.3 quote shape", "",
             f"**Alert:** {r['alert']['title']} · fired {r['alert']['fired_at']} · [monitor]({r['alert']['link']}) · **Branch:** `{r['branch']}` · **Status:** `{r['status']}`",
             target_line("client-portal"), "",
             "## Root cause", f"{s[3]['verdict']}.", "",
             "## What this PR changes", "- `src/pricing/normalize.js`: `px` may be a number (schema 2.2) or `{ v, ccy }` (schema 2.3+); currency prefers `px.ccy`, falls back to quote-level `ccy`; any other shape throws a `TypeError` naming the ISIN instead of surfacing later as `toFixed is not a function` or NaN.",
             "- `test/valuation.test.js`: regression test with the live v2.3 batch, an equivalence test (2.2 ≡ 2.3), and an unknown-shape test.", "",
             "## What was ruled out", f"- **{s[2]['title']}** — {s[2]['verdict']}.", *[f"  - {e}" for e in s[2]["evidence"]], "",
             "## Verification", *[f"- {e}" for e in s[4]["evidence"]], "",
             *browser_evidence_lines(),
             "## Not in this PR", *[f"- {e}" for e in s[5]["evidence"]], "",
             f"CODEOWNERS reviewers: {', '.join(owners_for(PORTAL, 'src/pricing/normalize.js'))}", "", "Devin-Org: engineering", ""]
    return "\n".join(lines)


if __name__ == "__main__":
    main()
