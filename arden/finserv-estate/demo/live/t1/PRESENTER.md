# demo_t1 presenter sheet: T+1 settlement migration

**Goal.** Arden moves EU, UK and CH regular-way trades from T+2 to T+1 on 2027-10-11 (SETTLE-4471). In one session Devin:
- finds every T+2 assumption across 20 repos and cites each one;
- migrates tranche 1 in 3 draft PRs;
- proves the result with golden-file parity;
- hands the one business decision, venue calendar vs TARGET2, to its owners.

About 15 minutes.

<!-- run-links: the setup session refills the Live column after each run -->
## Tabs (this run)

Use **Live**. Switch to **Backup** only if the live run stalls or a link isn't up yet, and say "this is the reference run".

| # | What it shows | Live | Backup |
| --- | --- | --- | --- |
| 1 | Devin working (setup already started it, prompt 1 is in it, nothing to paste) | [Live Devin session](https://devin-gtm.devinenterprise.com/sessions/a1c91077b05e400798db9860b626ed3f) | [BEFORE/AFTER video](https://github.com/COG-GTM/traderXCognitiondemos/pull/134#issuecomment-6406211177) |
| 2 | BEFORE: `default_cycle: T+2` hard-coded in the FIX mapping | [settlement-cycle.yaml L3](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/devin/1790864747-arden-finserv-estate/arden/iso20022-fix-messages/mapping/settlement-cycle.yaml#L3) | same link |
| 3 | Inventory: every T+2 assumption with file:line, owner and tranche | [Live inventory](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-t1-inventory.md) | [Reference inventory](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/reports/demo2-t1-inventory.md) |
| 4 | Tranche 1 draft PRs | [#80 core-dates](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80) · [#81 settlement-instruction](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/81) · [#82 iso20022-fix](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/82) | [#2](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2) · [#3](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/3) · [#4](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/4) |
| 5 | Parity and the 4 escalations | [Live parity](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-migration-report.md#3-parity--what-the-counterparty-would-see) | [Reference parity](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/reports/demo2-migration-report.md#3-parity--what-the-counterparty-would-see) |

## Beats

1. **The ask (tab 2).** "Go-live is 2027-10-11, and T+2 is hard-coded across the estate, like this line. Nobody has a full list."
2. **Hand it to Devin (tab 1).** "One prompt. I didn't tell Devin where to look."
3. **Inventory (tab 3).** Every row has a repo, file:line, owner and tranche.
   This run: 52 files, 124 citations, 16 of 20 repos. Read the numbers off the report, because they vary between runs.
4. **Tranche 1 (tab 4).** Open #80, then #81 and #82. "Date-gated on trade date, FIX 64/75 format unchanged, tests green. Drafts only: your engineers merge."
   Devin's own catch: it cut core-dates from `release/1.4` because `main` breaks the custody-gateway tag 64 contract.
5. **Parity (tab 5).** "Thirty golden trades: 20 changed, and every change is explained. Sixteen moved one business day earlier, as expected."
6. **The hard case (tab 5, the ESCALATE rows).** ARD-27Q4-0021, 0022, 0027 and 0028 are Christmas and New Year trades on Xetra and Paris.
   "The venue calendar and TARGET2 disagree. Devin doesn't pick a calendar. It names @ardencm/market-ops and the Head of Post-Trade."
7. **What's next.** Tranches 2 and 3 are in Devin's plan, not run live. The BOM stays at 1.4.0 until tranche 2.

**Close.** "One cited inventory, three reviewable PRs, parity proven, and the business decision with its owners."

Rules: never merge, deploy or post to Slack. Don't decide a calendar for its owner. Re-running setup closes this run's PRs #80 to #82.

## Why this demo (the point)

**The problem.** Every bank on T+2 has to get to T+1. The US already moved in May 2024, and the EU, UK and Switzerland follow on 11 October 2027.
The work isn't hard, but it's everywhere. "2 business days" lives in calendars, FIX mappings, cut-off configs, tests and spreadsheets, in repos owned by different teams.
Usually it takes weeks of people grepping before anyone can even size the programme.

**What Devin shows.**
1. **Scale with proof.** It reads the whole estate, 20 repos, and every finding comes back with a citation a reviewer can click.
2. **Safe change.** The tranche-1 PRs are date-gated on trade date, so one build is correct before and after go-live. The wire format stays unchanged, the tests are green, and the PRs are drafts that humans merge.
3. **Evidence, not assertion.** Golden-file parity shows exactly which trades move and why. You can rerun the check yourself.
4. **Judgment.** When the venue calendar and TARGET2 disagree, Devin doesn't guess. It escalates to the named owners with the evidence.

**The takeaway for the room.** Devin does weeks of estate-wide discovery and the first migration tranche in one session. It leaves an audit trail your risk and ops teams can check. Your people still make the business decisions and merge the code.
