# demo_incident: session map and evidence ledger

## Session map

| Beat | Session | What it proves |
|---|---|---|
| Rehearsal live run (2026-10-23) | https://devin-gtm.devinenterprise.com/sessions/b543691d7c52410694371774de90f552 | Fresh session, playbook `playbook-abc2582e18fd45558d56838a14101336`, prompt 1 pasted as is on seed `c846d3b` |
| Live fix PR from that run | https://github.com/COG-GTM/traderXCognitiondemos/pull/135 | Smallest fix against `demo/arden-live-before`, tests, ruled-out evidence, browser walk-through in a PR comment |
| Fallback (pre-built) | https://github.com/COG-GTM/traderXCognitiondemos/pull/122 and https://github.com/COG-GTM/traderXCognitiondemos/pull/118 | Same fix from earlier runs; `setup.sh --fallback` serves #122 |

The seed closes PRs on `demo/arden-live-before`, so PR #135 is closed by the next reseed. That is expected.

## Wall time (rehearsal run)

| Mark | UTC | Since prompt |
|---|---|---|
| Prompt sent | 02:59:07 | 0:00 |
| Playbook triggered | 02:59:10 | 0:03 |
| Fix PR #135 opened | 03:02:27 | 3:20 |
| Root cause and 31.4.2 ruled out reported | 03:04:13 | 5:06 |
| Incident pack attached, browser walk-through, session waiting | 03:08:41 | 9:34 |

## Evidence ledger

| Claim | Evidence | Where |
|---|---|---|
| The page fails like production on the seed | `setup.sh` serves the v2.3 batch: 500 `valuation_failed`, log shows `TypeError: n.px.toFixed is not a function` | `setup.sh` step "Verify BEFORE" |
| Deploy 31.4.2 is not the cause | Devin replayed the live batch on pre-deploy 31.4.1: same TypeError. 7 of 7 healthy valuations on 31.4.2 before 06:00. Failing line dates from 31.4.0 | PR #135 description, session messages |
| Root cause is ArdenFeed schema 2.3 | Vendor notice 2026-09-11 (effective 06:00 UTC, `px` becomes `{ v, ccy }`), `consumers.yaml` lists client-portal on 2.2 | PR #135 description |
| Fix is the smallest change | Only `src/pricing/normalize.js` (+16 -7) and `test/valuation.test.js` (+42) | PR #135 diff |
| Tests | 3 new tests fail on old code, 4 of 4 pass on the fix, lint passes, CI 2 of 2 green | PR #135 |
| Valued rows after the fix | v2.3 batch on PR #135 head: GB0002634946 148920.00 GBP, CH0012032048 216720.00 CHF, US0378331005 345600.00 USD | Recorded walk-through (PR #135 comment and the plugin PR) |
| Incident record leaves decisions to humans | Incident record, RCA: regulatory classification and client impact owned by ops risk; rollout owned by CODEOWNERS | Session attachments 01 to 04 |
| No prohibited actions | No merge, deploy, rollback or Slack post | Session timeline, PR #135 open |

## Known open points from the rehearsal

- Devin Review flagged two items on PR #135 (conflicting deprecated `ccy` rejected; row cache returns a cached row
  before validation). Devin left both for a human, as the org does not enable bot responses. Talk track covers the first.
- Devin's PR description says "PRs #118 and #121" for earlier runs; the fallback pair is #118 and #122.
