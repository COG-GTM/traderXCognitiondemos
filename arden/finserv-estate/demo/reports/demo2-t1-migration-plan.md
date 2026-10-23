# SETTLE-4471 — T+1 migration plan (programme T1-2027)

- **Markets in scope:** EU, UK and CH regular-way trades — venues in the estate: XLON (UK), XPAR, XETR (EU), XSWX (CH)
- **Go-live:** 2027-10-11 (trade date). Trades dated before 2027-10-11 keep T+2; XNYS/XNAS/XTSE stay T+1 (since 2024-05-28); other markets unchanged.
- **Invariants:** FIX tag 64/75 stay `LocalMktDate` (`yyyyMMdd`); tag 63 follows the cycle (`3` = T+2, `2` = T+1). Consumer contract tests are never weakened. No merges, no BOM pin moves.
- **Inventory:** `demo2-t1-inventory.md` (57 files / 105 citations / 17 of 20 repos).

## Tranche 1 — instruction-generation path (this ticket)

Dependency order: `core-dates` → `settlement-instruction-service` → `iso20022-fix-messages`.

| repo | change | reviewers (CODEOWNERS) |
|---|---|---|
| core-dates | `SettlementCycle.forTrade(mic, tradeDate)` date gate; `SettlementDates.settlementDate(tradeDate, mic)` and the CLI use it; `standard()`/`forMarket()` keep pre-go-live T+2 and are `@Deprecated`; tests for both sides of go-live. Release **from the line consumers run** (BOM pins 1.4.0 → cut 1.4.2 from `release/1.4`, not from `main`/1.5.0 which changed the tag 64 wire format). | @ardencm/post-trade-core |
| settlement-instruction-service | drop XLON/XPAR/XETR/XSWX `T_PLUS_2` pins; `cycleFor(mic, tradeDate)` falls back to the date-gated core-dates default; legacy `cycleFor(mic)` deprecated; tests both sides of go-live; custody-gateway contract test untouched and green. | @ardencm/settlements-core, @ardencm/market-ops |
| iso20022-fix-messages | `mapping/settlement-cycle.yaml` gains `effective_from: 2027-10-11` version (T+1, tag 63 `2`); pre-migration mapping and golden samples archived; validator picks the version from tag 75; new post-go-live samples. | @ardencm/messaging-standards, @ardencm/settlements-core |

Not in tranche 1: the BOM pin (`common-java-bom` stays at core-dates 1.4.0 — moving it needs a platform-eng ADR and tranche 2 consumers ready).

## Tranche 2 — downstream, schedules, wording, SQL (planned, not executed)

| repo | item | owner / sign-off |
|---|---|---|
| confirmation-service | hard-coded `T_PLUS_2` tag 63 for non-XN MICs (**must land before the BOM moves**, otherwise confirms carry a T+1 date with tag 63 `3`); T+2 narrative and PDF footer | @ardencm/post-trade-confirms, @ardencm/market-ops |
| allocation-service | `default-cycle: T+2`; give-up `plusDays(2)` calendar days (confirm the 2019 ops instruction first) | @ardencm/post-trade-allocations, post-trade ops |
| custody-gateway | CSD matching cut-off offset (T+1 12:00 for T+2); contract bump with T+1 examples (current examples pin XLON T+2) | @ardencm/custody-integration, @ardencm/market-ops |
| batch-scheduler-config | SETTLE_INSTR_GEN 06:00 next-morning run, AFFIRMATION_CHASER 09:00 T+1, RECALL_SWEEP (after GMSLA decision), `settlement-cutoffs.yaml` | @ardencm/batch-ops, @ardencm/settlements-core, @ardencm/market-ops |
| platform-terraform | `settle-instr-gen` EventBridge 06:00 cron mirrors Autosys | @ardencm/platform-eng |
| recon-job | `DEFAULT_LAG = 2`, late-match tolerance | @ardencm/ops-recon |
| risk-lib | `SETTLEMENT_LAG_DAYS` EU/UK/CH = 2 (date-gate) | @ardencm/risk-quant, @ardencm/settlements-core |
| legacy-stored-procs | `usp_CalcSettlementDate @Lag = 2`, `usp_FlagLateSettlements DATEADD(day, 2)` (CSDR penalty report), `vw_SettlementCalendar`; DBA change ticket | @ardencm/settlements-legacy-db |
| client-portal | `/api/settlement/summary` wording "Trades settle 2 business days after execution (T+2)" | @ardencm/client-digital |
| common-java-bom | move core-dates pin once tranche 2 consumers are ready (ADR) | @ardencm/platform-eng |

## Tranche 3 — decisions owed before any code changes (escalated)

| decision | evidence | decision owner |
|---|---|---|
| Calendar authority under T+1 (venue vs CSD vs TARGET2 cash calendar) | `core-dates/docs/t1-calendar-mismatch.md`, `holidays.yaml`; parity rows where venue and cash calendars disagree | @ardencm/market-ops + Head of Post-Trade |
| Ex-date regime for events announced before / record after go-live; dual-listed XSWX/XLON | `corporate-actions-service/docs/ex-date-policy.md`, CA-2291; dividend rows in parity report | Asset-servicing ops (CA-2291) |
| FX funding: spot stays T+2, so every EUR/GBP/CHF trade after go-live needs tom-next or pre-funding | `fx-funding-service/.../FxFundingService.java` | Treasury funding desk (Head of Treasury) |
| GMSLA recall notice (2 BD): under T+1 the recall deadline falls the day *before* the sale | `stock-loan-recall-service/.../RecallService.java`, README | Securities-lending desk head + Legal |
| Legacy position keeper (JDK 8, hard-coded `SETTLE_DAYS = 2`, London-only hand-kept calendar; PK-118) — patch in place vs re-platform | `legacy-position-keeper/src/.../SettleDateUtil.java` | Head of Post-Trade (system owner) |
| Give-up settlement rule ("trade date plus two", 2019) | `allocation-service/.../AllocationService.java` | Post-trade ops |
