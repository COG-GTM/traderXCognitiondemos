# demo_t1 prompts: T+1 settlement migration (live)

Setup first: `arden/finserv-estate/demo/live/t1/setup-t1.sh` on the kit branch. It prints READY with the seed URL and prompt 1.
Session: Devin, normal mode, repos `COG-GTM/traderXCognitiondemos`, `COG-GTM/Securities-Trade-Processing-System`, `COG-GTM/Springboot-BankApp`.
The prompt starts with `@playbook:` so the org playbook "FS T+1 settlement migration" (playbook-cbb39530e1f447758488999e30c93083) is attached. If you start the session through the API, pass `playbook_id` instead.

## Prompt 1 (paste as is)

<!-- prompt1 -->
```text
@playbook:playbook-cbb39530e1f447758488999e30c93083

Programme ticket T1-2027: move Arden to T+1 settlement for the 2027Q4 go-live (SETTLE-4471: EU, UK and CH regular-way trades, go-live 2027-10-11).

Repo: COG-GTM/traderXCognitiondemos, branch demo/t1-live-before; the estate kit is arden/finserv-estate. Read AGENTS.md first, then prepare the estate with `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh` (20 repos in ~/finserv-workspace).

1. Inventory every settlement-cycle assumption across the estate with a citation per file (repo, path, line, what it assumes, CODEOWNERS, tranche). Write it as a markdown report and tell me the headline counts: files, citations, repos.
2. Migrate tranche 1, the instruction-generation path (core-dates, settlement-instruction-service, iso20022-fix-messages). Date-gate on trade date, keep the FIX 64/75 wire format, keep each repo's tests green.
3. Run parity against demo/t1/golden/*.csv: `python3 arden/finserv-estate/demo/t1/parity.py --migrated <your core-dates CLI jar>` (the baseline defaults to the BOM-pinned jar). Explain every row that changed.
4. Escalate anything you cannot decide to its named owner, with the evidence. Do not choose between calendars.

Open draft PRs as AGENTS.md describes, one per repo you change. Never merge. Do not post to Slack.
```
<!-- /prompt1 -->

What Devin should come back with (presenter reference, from the kit scripts on the same BEFORE state):

| Step | Expected |
|---|---|
| Inventory | Kit regex reference: 37 files, 62 citations, 16 of 20 repos (tranche split 13 / 15 / 9). Devin's pre-run read wider: 57 files, 105 citations, 17 repos (docs, tests and archives included). |
| Tranche 1 | 3 draft PRs: core-dates (date-gated cycle, new minor version), settlement-instruction-service (BOM bump + tag 63 change), iso20022-fix-messages (mapping) |
| Parity | 30 golden trades: 20 changed (16 expected, 4 escalated), 10 unchanged |
| Escalation | ARD-27Q4-0021, 0022, 0027, 0028 (XETR / XPAR around 24 Dec and 31 Dec: venue calendar vs TARGET2 cash calendar) to @ardencm/market-ops and the Head of Post-Trade, per core-dates/docs/t1-calendar-mismatch.md |

Do not promise 37 / 62 on stage: Devin's inventory is broader than the kit's regex and the count varies per run. Hold Devin to a cited row for every file in the 37-file reference list, the three tranche 1 PRs with green tests, and the four escalations by trade ID and owner.

Pre-run (2026-10-23, session https://devin-gtm.devinenterprise.com/sessions/743880ab432043e9965d0ff94418fca0): about 11 minutes from prompt to three draft PRs; parity matched the reference exactly (20 / 16 / 10 / 4, same four trades).

## Prompt 2 (only if Devin has not escalated by itself)

```text
Four of the changed rows sit on dates where the venue calendar and the TARGET2 cash calendar disagree. Who owns that decision in this estate, and what evidence do they need from you? Do not pick a calendar.
```

## Prompt 3 (close)

```text
Summarise for the programme board: what changed, what is proven by tests and parity, what is waiting on which owner, and what tranche 2 and 3 contain. Link the PRs.
```

## Presenter's independent check

Run on the presenter machine after Devin's core-dates PR is up (the scripts check Devin, they do not produce the answer):

```bash
cd arden/finserv-estate
# build Devin's core-dates branch, then
python3 demo/t1/parity.py --migrated ~/finserv-workspace/core-dates/target/core-dates-<new version>-cli.jar
```

Same four ESCALATE lines means Devin's migration and the reference agree.

## Fallback

`arden/finserv-estate/demo/live/t1/fallback-t1.sh` runs the kit's inventory, tranche 1 migration and parity on the same BEFORE state (about 2 minutes) and writes `demo/reports/demo2-t1-inventory.md`, `demo2-parity.html`, `demo2-migration-report.md`. Present those as "what the run produces", not as Devin's live output.
