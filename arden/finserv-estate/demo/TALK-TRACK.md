# Arden Capital Markets financial-services demos

## Demo 1: fleet-wide CVE remediation

**Setup/reset:** `FS_SOURCES=~/fs-sources FS_SOURCE_MODE=remote scripts/reset.sh`, then `demo/run-demo1.sh`.

**Measured timing:** 25.6 seconds with default pacing, including reset.

**First spoken line:** The scanner has already done its job: 17 findings across 10 repositories, including the Artifactory token blast radius.

| Beat | Run | Show | Say | Expected |
|---|---|---|---|---|
| Findings | `demo/run-demo1.sh` | `demo/out/scanner/findings.json`, then `demo/reports/demo1-burndown.html` | The scanner found shared causes across the estate; Devin organizes independent remediation work. | 17 findings across 10 repositories |
| Review-ready work | Continue to the dashboard | Dashboard cards and per-finding outcomes | These changes are ready for CODEOWNERS review. No change has been merged. | 16 fixes ready, 1 with a documented deviation, 0 blocked |
| Evidence | Open `demo/reports/demo1-remediation-report.md` | SEC Item 1.05 and DORA Article 19 drafting aids | The evidence is prefilled, while materiality and notification decisions stay with people. | Run-derived findings and human-review fields |

### Hard case

Open the settlement-instruction-service panel at the top of `demo/reports/demo1-burndown.html`, then the matching PR artifact at `demo/out/scanner/prs/settlement-instruction-service/PR.md`. Devin tested the scanner recommendation, saw the custody-gateway tag-64 contract fail, pinned core-dates 1.4.1, and added `Tag64WireFormatPinTest`. The deviation is routed to the CODEOWNERS reviewer. Show both attempts and the failing test evidence.

## Demo 2: T+1 migration

**Setup/reset:** `FS_SOURCES=~/fs-sources FS_SOURCE_MODE=remote scripts/reset.sh`, then `demo/run-demo2.sh`.

**Measured timing:** 18.6 seconds with default pacing, including reset.

| Beat | Run | Show | Say | Expected |
|---|---|---|---|---|
| Inventory | `demo/run-demo2.sh` | `demo/reports/demo2-t1-inventory.md` | Devin maps settlement-cycle assumptions to owners and evidence. | 37 files, 62 citations and 16 repositories |
| Tranches | Review `demo/reports/demo2-t1-migration-plan.md` | Tranche assignments and decision owners | Mechanical changes are separated from decisions that require business authority. | 16 expected, 10 unchanged and 4 escalated trades |
| Parity | Continue through the parity step | `demo/reports/demo2-parity.html` | Each changed settlement instruction is compared against the previous behavior. | 30 trades; every difference classified |

### Hard case

Show the escalation summary at the top of `demo/reports/demo2-parity.html`. Trades 0021, 0022, 0027 and 0028 have venue-calendar and TARGET2 candidate dates. The named market-operations and post-trade owners receive the question; Devin records that no choice was made.

## Demo 3: ArdenFeed incident response

**Setup/reset:** `FS_SOURCES=~/fs-sources FS_SOURCE_MODE=remote scripts/reset.sh`, then `demo/run-demo3.sh`. `DEMO_PACE=0` disables pauses; the default is two seconds between investigation steps.

**Measured timing:** 21.3 seconds with default pacing, including reset.

| Beat | Run | Show | Say | Expected |
|---|---|---|---|---|
| Alert | `demo/run-demo3.sh` | Terminal alert and `demo/out/incident/incident-input.json` | The alert points at the 31.4.2 deployment, so test that lead before changing code. | 31.4.2 memoisation deploy at 21:05; monitor fires at 06:04 UTC |
| Investigation | Continue through the five investigation steps | Terminal output and `demo/out/incident/investigation.json` | Replay the live batch on the previous release, then compare the feed shape. | Deploy ruled out; ArdenFeed schema 2.3 identified as the cause |
| Client view | Open `http://localhost:3210/` on main, then the incident fix branch | Main shows the valuation error; the fix branch renders the portfolio table | Clients see a clear failure before the fix and three valued positions after it. | HTTP 500 `valuation_failed` before; HTTP 200 with three rows after |
| Record | Open `demo/reports/demo3-incident-timeline.html`, `demo/reports/demo3-incident-record.md` and `demo/reports/demo3-rca.md` | Verdict at the top and 25 September timeline | The incident record is deterministic; rollout and regulatory classification remain human decisions. | Investigation ends before 08:00 UTC; fix branch tests and lint pass |

### Hard case

The last deploy looks responsible but fails identically on the live batch when replayed before deployment. ArdenFeed schema 2.3 changed `px` from a number to `{v, ccy}`. A rollback would have kept the portal down; the fix branch accepts both schemas and rejects unknown shapes.
