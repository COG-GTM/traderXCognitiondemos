# demo_t1: T+1 settlement migration across the Arden estate

**Goal:** EU, UK and Swiss regular-way trades move from T+2 to T+1 on 2027-10-11 (SETTLE-4471). In one prompt Devin:
- finds every T+2 assumption across 20 repos, with a citation for each;
- migrates tranche 1 in 3 draft PRs;
- proves the result with golden-file parity;
- hands the one business decision, venue calendar vs TARGET2, to its owners.

About 13 min. Nothing is merged, deployed or posted.

**Say first:** "This is a regulatory date change across a whole bank. Nobody has a list of where 'two days' is hard-coded."

## Tabs
1. [Live session](https://devin-gtm.devinenterprise.com/sessions/a1c91077b05e400798db9860b626ed3f): the setup session started it, and prompt 1 is already in it. Open it as soon as you start talking.
2. [BEFORE: T+2 hard-coded](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/devin/1790864747-arden-finserv-estate/arden/iso20022-fix-messages/mapping/settlement-cycle.yaml#L3): `default_cycle: T+2` in the FIX mapping.
3. [Golden trades (30)](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/t1-live-before/arden/finserv-estate/demo/t1/golden/trades-2027Q4.csv): the Q4 book that parity is run on.
4. Live PRs (appear at about 12:30): [#80 core-dates](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80), [#81 settlement-instruction-service](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/81), [#82 iso20022-fix-messages](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/82). Not open yet? Use [STPS #2](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2), the always-open reference PR with the BEFORE/AFTER parity video at the top of its description.

## Beats
| Time | Beat | Show | PR |
|---|---|---|---|
| 0:00 | Go-live is 2027-10-11, and T+2 is hard-coded across the estate, like this line. | Tab 2 | |
| 0:30 | One prompt; I didn't tell Devin where to look. | Tab 1, the prompt | |
| 1:00 | Devin reads AGENTS.md first, like a new engineer, then resets the 20 repos. | Tab 1 | |
| ~4:00 | Inventory: every row is a repo, a file:line, an owner and a tranche. Read the counts off the report, because they vary between runs. | [Live inventory](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-t1-inventory.md) (this run: 52 files, 124 citations, 16 of 20 repos) | |
| ~8:00 | Devin's catch: core-dates `main` breaks the custodian's FIX tag 64 contract, so Devin cuts from `release/1.4` instead. | Tab 1 | [#80](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80) |
| ~12:30 | Tranche 1: date-gated on trade date, FIX 64/75 unchanged, tests green, drafts only. | Tab 4 | [#80](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80), then [#81](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/81) and [#82](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/82) |
| ~13:00 | Parity on 30 golden trades: 20 changed and every change explained; 16 settle one business day earlier, as expected. | [Live parity](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-migration-report.md#3-parity--what-the-counterparty-would-see), tab 3 | |
| ~13:30 | The hard case: ARD-27Q4-0021, 0022, 0027 and 0028 are Christmas and New Year trades on Xetra and Paris. The venue calendar and TARGET2 disagree. Devin doesn't pick; it names @ardencm/market-ops and the Head of Post-Trade. | [Escalations](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792978494-settle-4471-t1-reports/arden/finserv-estate/demo/reports/demo2-migration-report.md#4-escalations--decisions-the-code-does-not-make) | |
| ~14:00 | What's next: tranches 2 and 3 are in Devin's plan with owners, not run live. | Inventory, tranche column | |

- **Close:** "One cited inventory, three reviewable PRs, parity proven, and the business decision left with its owners."

## Which PR
- **Normal:** this run's [#80](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/80), [#81](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/81) and [#82](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/82), opened live in tab 1. Show them in that order, which is also the merge order.
- **Stalled, or past 13:00:** [STPS #2](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2) (core-dates, video at the top), [#3](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/3) (settlement-instruction-service) and [#4](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/4) (iso20022-fix-messages). Say "the run we did while preparing the demo". Reseeding never closes them.
- **Never merge any of them.**

## Prompt (already sent to the live session, word for word)
```
Programme ticket T1-2027: move Arden to T+1 settlement for the 2027Q4 go-live (SETTLE-4471: EU, UK and CH regular-way trades, go-live 2027-10-11).

Repo: COG-GTM/traderXCognitiondemos, branch demo/t1-live-before; the estate kit is arden/finserv-estate. Read AGENTS.md first, then prepare the estate with `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh` (20 repos in ~/finserv-workspace).

1. Inventory every settlement-cycle assumption across the estate with a citation per file (repo, path, line, what it assumes, CODEOWNERS, tranche). Write it as a markdown report and tell me the headline counts: files, citations, repos.
2. Migrate tranche 1, the instruction-generation path (core-dates, settlement-instruction-service, iso20022-fix-messages). Date-gate on trade date, keep the FIX 64/75 wire format, keep each repo's tests green.
3. Run parity against demo/t1/golden/*.csv: `python3 arden/finserv-estate/demo/t1/parity.py --migrated <your core-dates CLI jar>` (the baseline defaults to the BOM-pinned jar). Explain every row that changed.
4. Escalate anything you cannot decide to its named owner, with the evidence. Do not choose between calendars.

Open draft PRs as AGENTS.md describes, one per repo you change. Never merge. Do not post to Slack.
```

## Check
- [ ] Every inventory row has a file:line. Don't promise a count.
- [ ] 3 draft PRs: core-dates, settlement-instruction-service and iso20022-fix-messages, with tests green.
- [ ] Parity: 30 trades, 20 changed, 16 expected, 10 unchanged, 4 escalated.
- [ ] 0021, 0022, 0027 and 0028 are escalated to their owners, not decided.

**If Devin picks a calendar:** send prompt 2 from [PROMPTS.md](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/live/t1/PROMPTS.md).

**Fallback:** "Here's the reference run we did while preparing the demo." Show [STPS #2](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/2) to [#4](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/4), with the [reference inventory](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/reports/demo2-t1-inventory.md) and the [reference parity](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/reports/demo2-migration-report.md#3-parity--what-the-counterparty-would-see).

## Why this demo
- **The problem:** every bank on T+2 has to get to T+1; the US moved in May 2024, and the EU, UK and Switzerland follow on 11 October 2027. The change isn't hard, but it's everywhere: calendars, FIX mappings, cut-offs, tests and client wording, in repos owned by different teams. Usually it takes weeks of grepping before anyone can size the programme.
- **What Devin shows:** in one session it delivers an estate-wide inventory with a citation a reviewer can click for every finding, plus the first migration tranche as small draft PRs. The PRs are date-gated so one build is correct before and after go-live, and the wire format doesn't change.
- **The judgment moment:** parity proves which trades move and why. Where the venue and TARGET2 calendars disagree, Devin escalates to the named owners with the evidence instead of guessing.
- **Takeaway:** "Weeks of discovery and the first tranche in one session, with an audit trail risk and ops can check. Your people still make the business decisions and merge the code."
