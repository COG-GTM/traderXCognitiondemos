# demo_t1 talk track: T+1 settlement migration with Devin, live

Ordered the way Garret reads it: Project / Talk track / Repo / Wow moment / Decisions.
Skipped for this demo, on purpose: customer research, Hero Demo A/B, companion deck.

## Project

Arden Capital Markets (synthetic) must move EU, UK and Swiss regular-way trades from T+2 to T+1 for go-live on 11 Oct 2027 (ticket T1-2027, programme SETTLE-4471).
The settlement cycle is hard-coded across 20 repos in Java, Python, YAML, cron and client wording. Nobody has a list.

## Stage version (decision)

Live on stage: inventory, tranche 1 migration (3 repos), parity against the golden trades, and the escalation.
Pre-built, shown not run: tranches 2 and 3. Devin's own inventory names them and assigns owners; the presenter does not ask Devin to migrate them on stage.
Why: the full 37 files across 16 repos is a long run; tranche 1 is the part that changes settlement dates on the wire, and parity proves it. The rest is the same work at larger scale.

## Talk track

| Beat | Presenter says | Devin is doing | Live or pre-built |
|---|---|---|---|
| 0. Set up (before the room) | | `setup-t1.sh` prints READY with the seed URL | pre-built |
| 1. The ask | "This is a regulatory date change across a whole estate. Watch Devin find every place the bank assumes two days." Paste prompt 1. | reads AGENTS.md and the playbook, resets the workspace | live |
| 2. Inventory | "Every row is a file and a line you can open. Devin is citing, not summarising." | greps and reads 20 repos, writes the inventory with CODEOWNERS and tranche per file | live |
| 3. Tranche 1 | "Devin changes only the instruction path: the date library, the instruction service, the FIX/ISO mapping. Date-gated on trade date, so trades booked before go-live still settle T+2." | edits core-dates, bumps the BOM, changes FIX tag 63, keeps tags 64/75 wire format, runs each repo's tests, opens draft PRs | live |
| 4. Parity | "Thirty golden trades from the Q4 book. Every changed date has to be explained." | runs `parity.py` old jar vs new jar | live |
| 5. The hard case | "Four trades around Christmas and New Year on Xetra and Euronext Paris: the venue says one date, TARGET2 cash says another. Devin does not pick. Devin names the owner." | escalates ARD-27Q4-0021, 0022, 0027, 0028 to @ardencm/market-ops and the Head of Post-Trade with the evidence | live |
| 6. Independent check | "I don't take Devin's word for it." Run `parity.py --migrated <Devin's jar>` on the presenter machine. | | live, presenter |
| 7. Rest of the programme | "Tranches 2 and 3 are in Devin's inventory with owners. Tranche 3 needs business decisions first, so Devin stops there." | prompt 3 summary | pre-built scope, live summary |

## Repo

- Seed (BEFORE, no answers): https://github.com/COG-GTM/traderXCognitiondemos/tree/demo/t1-live-before
- Kit (scripts, this file, fallback): `arden/finserv-estate/demo/live/t1/` on `devin/1792724600-demo-arden-live`
- Estate repos: `arden/` in traderXCognitiondemos, Securities-Trade-Processing-System, Springboot-BankApp on `devin/1790864747-arden-finserv-estate`
- Golden file: `arden/finserv-estate/demo/t1/golden/trades-2027Q4.csv` (30 trades)
- Playbook: playbook-cbb39530e1f447758488999e30c93083 (FS T+1 settlement migration)

## Wow moment

Devin finishes the migration, parity shows 4 rows Devin cannot explain by the cycle change, and Devin stops and routes them to market ops with the two calendar dates side by side. The point: Devin did the mechanical 90% and refused the one decision that belongs to the business.

## Decisions (owed by the business, not Devin)

- Venue calendar vs TARGET2 cash calendar on EU holiday-adjacent dates: @ardencm/market-ops + Head of Post-Trade (`core-dates/docs/t1-calendar-mismatch.md`).
- Anything Devin's inventory puts in tranche 3 waits for its named owner.

## If it goes wrong

- Devin is slow: talk over beats 2 and 3 using `PROMPTS.md` expected numbers; skip to beat 6 once the core-dates PR exists.
- Devin stalls or fails: run `fallback-t1.sh` and present its reports as the reference run, not as Devin's output.
- Devin picks a calendar instead of escalating: send prompt 2.
