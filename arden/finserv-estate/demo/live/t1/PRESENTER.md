# demo_t1 presenter sheet: T+1 settlement migration (pin this)

**Goal.** Arden must move EU, UK and CH regular-way trades from T+2 to T+1 for the 2027-10-11 go-live (T1-2027 / SETTLE-4471).
In about 11 minutes Devin will:

- find every settlement-cycle assumption across 20 repos, with a file:line citation for each;
- migrate tranche 1 in 3 draft PRs;
- show golden-file parity;
- escalate the 4 trades where the venue calendar and TARGET2 disagree. Devin does not pick a calendar.

**Who:** a post-trade or operations technology lead. **Length:** about 15 minutes.

## Tabs to open (in this order)

| Tab | Link | What it is |
| --- | --- | --- |
| 1. Live Devin run | the `Live session` link the setup session sends you (or [Devin home](https://devin-gtm.devinenterprise.com/) + prompt 1) | Prompt 1 is already running. Present from here. |
| 2. Prompt 1 | [PROMPTS.md, prompt 1](PROMPTS.md#prompt-1-paste-as-is) | If you start your own run: new session, normal mode, repos traderXCognitiondemos + Securities-Trade-Processing-System + Springboot-BankApp, paste prompt 1. |
| 3. Seed (BEFORE) | [demo/t1-live-before](https://github.com/COG-GTM/traderXCognitiondemos/tree/demo/t1-live-before/arden/finserv-estate/demo/t1) | The golden trades and the parity checker. There is no inventory and no migration yet. |
| 4. Video (default link) | [PR #134, demo_t1 BEFORE/AFTER video](https://github.com/COG-GTM/traderXCognitiondemos/pull/134#issuecomment-6406211177) | Parity goes from 0 changed to 20 changed with 4 escalated. PR #134 never closes. |
| 5. Reference PRs | [STPS #2 core-dates](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2), [#3 settlement-instruction-service](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/3), [#4 iso20022-fix-messages](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/4) | Standing tranche-1 PRs, open. `seed-t1.sh` never closes them. |

## Beats (and the PR or link for each)

1. **The ask (tab 3).** "Twenty repos and one regulatory date. Nobody knows where T+2 is hard-coded."
   Show the golden trade file, then switch to tab 1.
2. **Hand it to Devin (tab 1).** Prompt 1 is already in the session.
   "I haven't told Devin where the T+2 assumptions are. It reads the estate the way a new engineer would."
3. **Inventory (tab 1).** Open Devin's inventory report. Every row has repo, path, line, CODEOWNERS and tranche.
   Read the headline counts from Devin's message. **Don't quote a number in advance; it varies between runs.**
4. **Tranche 1 PRs.** Open the 3 draft PRs Devin posts in tab 1: core-dates, settlement-instruction-service and iso20022-fix-messages.
   The change is date-gated on trade date, keeps the FIX 64/75 wire format, and the tests are green.
   **If the live PRs aren't up yet, use tab 5: STPS #2, #3 and #4.**
   "Devin opens drafts. Your engineers review and merge."
5. **Parity (tab 4).** Play the video, or show Devin's parity output in tab 1.
   BEFORE: 30 trades, 0 changed. AFTER: 20 changed, 16 expected, 10 unchanged, 4 escalated.
   "Every changed row is explained."
6. **The hard case.** ARD-27Q4-0021, 0022, 0027 and 0028 are Christmas and New Year trades on Xetra and Euronext Paris.
   The venue calendar gives one settlement date and TARGET2 cash gives another.
   "Devin doesn't pick. It names the owners, @ardencm/market-ops and the Head of Post-Trade, and attaches the evidence."
7. **Your own check (optional, terminal).** Run `python3 arden/finserv-estate/demo/t1/parity.py --migrated <jar from Devin's core-dates PR>`.
   You should get the same 20 / 16 / 10 / 4.
8. **The rest of the programme.** Tranches 2 and 3 come from Devin's own plan in the inventory report. They are not run live.

**Close.** "One cited inventory, three reviewable PRs, parity on the golden file, and the one business decision sent to the people who own it."

## Fallback

- **Live run stalls:** use tab 4 (video) and tab 5 (STPS #2 to #4). Say they are the reference run.
- **You want fresh reports on the VM:** run `arden/finserv-estate/demo/live/t1/fallback-t1.sh`. It takes about 2 minutes and writes
  [`demo2-t1-inventory.md`](../../reports/demo2-t1-inventory.md), [`demo2-migration-report.md`](../../reports/demo2-migration-report.md) and `demo2-parity.html`.
  Say it is the kit's reference run, not Devin's live output.

## Rules

- Never merge, deploy or post to real Slack.
- Don't decide between the venue calendar and TARGET2 for an owner.
- Rerunning setup (`setup-t1.sh`) closes the live-run PRs from the previous run. It keeps STPS #2 to #4 and kit PR #134.
