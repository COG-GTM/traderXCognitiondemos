# demo_t1 presenter sheet: T+1 settlement migration

**Goal.** Arden moves EU, UK and CH regular-way trades from T+2 to T+1 on 2027-10-11 (SETTLE-4471). In one session Devin:
- finds every T+2 assumption across 20 repos and cites each one;
- migrates tranche 1 in 3 draft PRs;
- proves the result with golden-file parity;
- hands the one business decision, venue calendar vs TARGET2, to its owners.

About 15 minutes.

<!-- run-links: the setup session refills this table after each run -->
## Tabs (this run)

| # | Open | What it shows |
| --- | --- | --- |
| 1 | [Live Devin session](https://devin-gtm.devinenterprise.com/sessions/a1c91077b05e400798db9860b626ed3f) | Setup already started it with prompt 1. You don't paste anything. Present Devin's work and final message from here. |
| 2 | [BEFORE: T+2 hard-coded](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/devin/1790864747-arden-finserv-estate/arden/iso20022-fix-messages/mapping/settlement-cycle.yaml#L3) | `default_cycle: T+2` in the FIX mapping. This is one of the 124 places Devin found. |
| 3 | Live PRs: [#80 core-dates](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80), [#81 settlement-instruction-service](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/81), [#82 iso20022-fix-messages](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/82) | Devin's tranche-1 drafts from this run. |
| 4 | [Parity + escalations](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-migration-report.md#parity) | This run's golden-file results: 20 changed, 16 expected, 4 escalated. Also: [inventory](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-t1-inventory.md). |

## Beats

1. **The ask (tab 2).** "Go-live is 2027-10-11, and T+2 is hard-coded across the estate, like this line. Nobody has a full list."
2. **Hand it to Devin (tab 1).** "One prompt. I didn't tell Devin where to look."
3. **Inventory (tab 4, inventory link).** Every row has a repo, file:line, owner and tranche.
   This run: 52 files, 124 citations, 16 of 20 repos. Read the numbers off the report, because they vary between runs.
4. **Tranche 1 (tab 3).** Open #80, then #81 and #82. "Date-gated on trade date, FIX 64/75 format unchanged, tests green. Drafts only: your engineers merge."
   Devin's own catch: it cut core-dates from `release/1.4` because `main` breaks the custody-gateway tag 64 contract.
5. **Parity (tab 4).** "Thirty golden trades: 20 changed, and every change is explained. Sixteen moved one business day earlier, as expected."
6. **The hard case (tab 4, the ESCALATE rows).** ARD-27Q4-0021, 0022, 0027 and 0028 are Christmas and New Year trades on Xetra and Paris.
   "The venue calendar and TARGET2 disagree. Devin doesn't pick a calendar. It names @ardencm/market-ops and the Head of Post-Trade."
7. **What's next.** Tranches 2 and 3 are in Devin's plan, not run live. The BOM stays at 1.4.0 until tranche 2.

**Close.** "One cited inventory, three reviewable PRs, parity proven, and the business decision with its owners."

## Backup (only if the live run fails or its PRs aren't up yet)

Use **[STPS #2](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2)**. It's an earlier reference run of the same tranche-1 change, and a BEFORE/AFTER parity video plays at the top of the description.
[#3](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/3) and [#4](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/4) are its siblings. Reseeding never closes them. Say "this is the reference run", not Devin live.

Rules: never merge, deploy or post to Slack. Don't decide a calendar for its owner. Re-running setup closes this run's PRs #80 to #82.
