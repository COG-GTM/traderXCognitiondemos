# SETTLE-4471 T+1 migration plan (programme T1-2027)

**Scope, as given:** EU, UK and CH regular-way trades. **Go-live: 2027-10-11** (trade date). Venues in the Arden estate: XLON (UK), XPAR, XETR (EU), XSWX (CH). XNYS/XNAS/XTSE are already T+1 and out of scope; any other market keeps its current cycle.

**Rules:** gate on trade date (before 2027-10-11 = T+2, on/after = T+1). FIX tag 64/75 stay `LocalMktDate` `yyyyMMdd`; tag 63 follows the cycle (`3` = T+2, `2` = T+1). No BOM pin moves, nothing merges. Inventory: `demo2-t1-inventory.md` (52 files, 124 citations, 16 of 20 repos).

## Tranche 1: instruction-generation path (executed)

| order | repo | change | reviewers (CODEOWNERS) |
|---|---|---|---|
| 1 | core-dates | `SettlementCycle.forMarket(mic, tradeDate)` and `standard(tradeDate)`; `SettlementDates.settlementDate(tradeDate, mic)` and the CLI use them. `standard()` / `forMarket(mic)` deprecated, still T+2. Release **1.4.2 cut from `release/1.4`** (deviation, see below). | @ardencm/post-trade-core |
| 2 | settlement-instruction-service | `cycleFor(mic, tradeDate)` (explicit pin wins, else core-dates gate); drop the XLON/XPAR/XETR/XSWX `T_PLUS_2` pins; service pom overrides core-dates to 1.4.2. | @ardencm/settlements-core, @ardencm/market-ops (application.yml) |
| 3 | iso20022-fix-messages | EU/UK/CH mapping entries T+1 / 63=`2` with `effective_from: 2027-10-11` and `before:` T+2 / `3`; validator picks the entry by tag 75; pre-migration mapping and samples archived under `mapping/archive/` and `samples/archive/pre-2027-10-11/`. | @ardencm/messaging-standards, @ardencm/settlements-core (mapping/) |

**Deviation:** attempt A built the change on core-dates `main` (1.5.0 line). The consumer contract `CustodyGatewayContractTest` failed: `tag 64 must match ^[0-9]{8}$ but was 2027-10-13` (1.5.0 switched `FixDates` to ISO-8601). The contract was not touched. Attempt B cut 1.4.2 from `release/1.4` (the line the BOM pin 1.4.0 and the 1.4.1 security patch are on): contract and all tests green.

**BOM:** not moved (still 1.4.0). Only settlement-instruction-service takes 1.4.2 via its own pom. Moving the BOM to 1.4.2 changes every two-argument `SettlementDates.settlementDate` caller; run against 1.4.2 today, confirmation-service, allocation-service, fx-funding-service and stock-loan-recall-service each fail one T+2-pinned test (`expected 2027-10-13 but was 2027-10-12`). corporate-actions-service passes. So the BOM moves only after tranche 2 and the tranche 3 decisions. Owner: @ardencm/platform-eng with consumer CODEOWNERS sign-off (ADR 0007).

## Tranche 2: downstream consumers, schedules, wording (planned, not executed)

| repo | item | owner |
|---|---|---|
| confirmation-service | tag 63 hard-coded `T_PLUS_2` for non-`XN*` MICs while the settlement date comes from core-dates: once the BOM moves, confirms would show a T+1 date with 63=3. Narrative/footer say T+2 / "two business days". **Blocking for the BOM move.** | @ardencm/post-trade-confirms, @ardencm/market-ops |
| allocation-service | `default-cycle: T+2`; give-ups use `plusDays(2)` calendar days | @ardencm/post-trade-allocations, @ardencm/market-ops |
| custody-gateway | matching cut-off comment and `cutoff-offset-days: 1` keyed to T+2; contract examples T+2 (add a T+1 example by contract bump; tag 64 pattern unchanged) | @ardencm/custody-integration, @ardencm/market-ops |
| batch-scheduler-config, platform-terraform | `SETTLE_INSTR_GEN` 06:00 next morning and `AFFIRMATION_CHASER` 09:00 T+1 assume T+2; under T+1 the affirmation deadline is trade date (parity shows it for every in-scope post-go-live trade); EU/UK/CH cut-offs `12:00 T+1` | @ardencm/batch-ops, @ardencm/settlements-core, @ardencm/platform-eng |
| recon-job | `DEFAULT_LAG = 2` for every non-NA venue, would mark T+1 settlements early/BREAK | @ardencm/ops-recon |
| risk-lib | `SETTLEMENT_LAG_DAYS` EU/UK/CH = 2, region keyed, no date gate (JP stays 2) | @ardencm/risk-quant, @ardencm/settlements-core |
| legacy-stored-procs | `@Lag INT = 2`, `DATEADD(day, 2, ...)` in late-settlement flag and naive statement view | @ardencm/settlements-legacy-db |
| legacy-position-keeper | `SETTLE_DAYS = 2` on JDK 8, cannot take core-dates (PK-118) | @ardencm/settlements-legacy |
| client-portal | `/api/settlement/summary`: "Trades settle 2 business days after execution (T+2)" | @ardencm/client-digital |

Double-settlement day: trades of Fri 2027-10-08 (T+2) and Mon 2027-10-11 (T+1) both settle Tue 2027-10-12. Ops capacity check for that day sits with @ardencm/market-ops.

## Tranche 3: decisions owed (escalated, code unchanged)

| # | decision | evidence | candidates | owner |
|---|---|---|---|---|
| E1 | Which calendar is authoritative for settlement date when venue and cash (TARGET2) calendars disagree | parity rows ARD-27Q4-0021, -0022, -0027, -0028; `core-dates/docs/t1-calendar-mismatch.md`, `holidays.yaml` | 0021 XETR: venue 2027-12-27 / cash 2027-12-24 · 0022 XPAR: 2027-12-28 / 2027-12-24 · 0027 XPAR: 2028-01-03 / 2027-12-31 · 0028 XETR: 2028-01-03 / 2027-12-31 | @ardencm/market-ops + Head of Post-Trade |
| E2 | Ex-date regime for events announced before and recorded after go-live, and for dual listings | CA-27Q4-0101 (XLON), CA-27Q4-0102 (XSWX primary, XLON secondary); `corporate-actions-service/docs/ex-date-policy.md`, `ExDateCalculator.java` | 0101: 2027-10-21 (T+2 rule) / 2027-10-22 (T+1 rule) · 0102: 2027-10-14 / 2027-10-15 | Asset-servicing ops, CA-2291 |
| E3 | FX funding once securities settle T+1 but FX spot stays T+2 (tom-next / pre-fund) | `fx-funding-service/FxFundingService.java` `FX_SPOT_DAYS = 2`, README | keep spot T+2 and deal tom-next / pre-fund / other | Treasury (FX funding), via @ardencm/treasury-tech |
| E4 | GMSLA recall notice period (2 business days) under T+1: a trade-date recall no longer lands by settlement | `stock-loan-recall-service/RecallService.java` `RECALL_NOTICE_BUSINESS_DAYS = 2`, README | renegotiate notice to 1 day / recall before trade / accept fails | Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending |

## Engineering follow-ups (not business decisions)

- core-dates `main` (1.5.0) carries the ISO tag 64/75 change that breaks the custody contract; forward-porting tranche 1 to `main` needs @ardencm/post-trade-core, @ardencm/messaging-standards and @ardencm/custody-integration to agree first.
- Other EU venues (XAMS, XBRU, XMIL, ...) are not in the estate calendars; add them to `EU_UK_CH_MICS` with their calendars when onboarded.
