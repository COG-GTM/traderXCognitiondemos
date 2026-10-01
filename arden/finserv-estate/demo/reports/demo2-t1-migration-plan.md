# T+1 migration plan — SETTLE-4471 (EU/UK/CH T+1, go-live 11 Oct 2027)

Go-live **2027-10-11** (EU, UK, Switzerland; US/CA already T+1 since 28 May 2024). Plan generated 2026-09-25T21:19:32Z from the inventory in `demo2-t1-inventory.md`. Three tranches: code the instruction path first (that is what counterparties see), then consumers, and park anything that is a business decision until the named owner rules.

Principle: **date-gate, don't flip.** Every change resolves the cycle from the trade date so pre-go-live trades still settle T+2, back-dated corrections keep working, and the same build runs before and after 11 Oct 2027.

## Tranche 1 — Instruction-generation path (core-dates → settlement-instruction-service → message mapping)

Executed by `demo/t1/migrate.py --tranche 1`; verified by the golden-file parity harness (`demo/t1/parity.py`). One PR per repo.

### core-dates
- `src/main/java/com/ardencm/posttrade/dates/FixDates.java` L26 — No change — hit is the FIX tag 63 code table (3 = T+2), which must keep all values. False positive, kept so the reviewer sees it was considered.  
  owners: @ardencm/post-trade-core
- `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java` L10 — Date-gate standard(): T+2 before go-live, T+1 from 11 Oct 2027; add standard(LocalDate) / forMarket(mic, date). Keep the no-arg forms deprecated so the compiler flags remaining callers.  
  owners: @ardencm/post-trade-core
- `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java` L11 — No change; inherits the date-gated default. Used by the parity harness.  
  owners: @ardencm/post-trade-core
- `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java` L21 — Existing T+2 expectations become pre-go-live cases; add post-go-live cases incl. the 8→12 Oct double-settlement day.  
  owners: @ardencm/post-trade-core
- `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java` — Resolve cycle from trade date, not a static default.  
  owners: @ardencm/post-trade-core

### iso20022-fix-messages
- `mapping/settlement-cycle.yaml` L2 — EU/UK/CH markets → cycle T+1, tag 63 = 2, effective 11 Oct 2027. Custodians consume this file — the change *is* the counterparty notice.  
  owners: @ardencm/messaging-standards, @ardencm/settlements-core
- `README.md` — Update wording.  
  owners: @ardencm/messaging-standards
- `mapping/archive/settlement-cycle-pre-t1.yaml` — No change — this is the pre-T+1 mapping archived by tranche 1 so custodians can diff old vs new.  
  owners: @ardencm/messaging-standards, @ardencm/settlements-core

### settlement-instruction-service
- `src/main/resources/application.yml` L11 — Drop the explicit XLON/XPAR/XETR/XSWX T_PLUS_2 pins so venues fall back to the date-gated default; keep North America explicit.  
  owners: @ardencm/settlements-core, @ardencm/market-ops
- `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java` L31 — No change — this is the consumer contract (tag 64 yyyyMMdd) the migration must keep green.  
  owners: @ardencm/settlements-core
- `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java` L15 — Split into pre/post go-live cases; contract tests (tag 64 LocalMktDate) unchanged.  
  owners: @ardencm/settlements-core
- `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java` — cycleFor(mic, tradeDate) — fallback is SettlementCycle.standard(tradeDate).  
  owners: @ardencm/settlements-core
- `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java` — Pass trade date into cycleFor; tag 63 follows the resolved cycle (3→2).  
  owners: @ardencm/settlements-core

## Tranche 2 — Downstream consumers, schedulers, recon, client-facing wording

One PR per repo, same date-gating pattern, each with its own repo tests. Scheduler/IaC changes need a CAB ticket. Not executed in this demo.

### allocation-service
- `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java` L17 — GIVE_UP_SETTLEMENT_DAYS uses plusDays (calendar days, not business days) — latent bug independent of T+1; route through core-dates.  
  owners: @ardencm/post-trade-allocations
- `src/main/resources/application.yml` L8 — default-cycle T+2 → T+1 for client confirm wording.  
  owners: @ardencm/post-trade-allocations, @ardencm/market-ops

### batch-scheduler-config
- `calendars/settlement-cutoffs.yaml` L3 — Affirmation and CSD-matching cut-offs move from 12:00 T+1 to T evening (venue-specific; ops to confirm exact times with each CSD).  
  owners: @ardencm/batch-ops
- `jil/settlements.jil` L18 — SETTLE_INSTR_GEN must run on T (evening) not T+1 06:00; AFFIRMATION_CHASER on T; RECALL_SWEEP needs re-timing (see stock-loan decision).  
  owners: @ardencm/batch-ops, @ardencm/settlements-core

### client-portal
- `src/server.js` L18 — Client-facing FAQ string says trades settle T+2.  
  owners: @ardencm/client-digital

### confirmation-service
- `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java` L23 — Replace `mic.startsWith("XN") ? T_PLUS_1 : T_PLUS_2` with core-dates forMarket(mic, tradeDate).  
  owners: @ardencm/post-trade-confirms
- `src/main/resources/application.yml` L7 — Client narrative and footer text state T+2 and a 12:00 T+1 affirmation deadline.  
  owners: @ardencm/post-trade-confirms, @ardencm/market-ops
- `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java` L12 — Test fixture narrative hard-codes 'T+2 basis'; becomes a pre-go-live case.  
  owners: @ardencm/post-trade-confirms

### custody-gateway
- `src/main/resources/application.yml` L7 — Comment documents the T+1 12:00 CET matching cut-off for T+2 settlement; cut-off moves to T.  
  owners: @ardencm/custody-integration, @ardencm/market-ops

### legacy-stored-procs
- `README.md` L5 — README enumerates the three lag-bearing objects; no automated tests — DBA smoke only, so the PR needs a UAT ticket.  
  owners: @ardencm/settlements-legacy-db
- `procs/usp_FlagLateSettlements.sql` L4 — DATEADD(day, 2, TradeDate) → 1 from go-live; feeds the CSDR penalty report, so the change must be dated not flipped.  
  owners: @ardencm/settlements-legacy-db
- `views/vw_SettlementCalendar.sql` L4 — NaiveSettlementDate = DATEADD(day, 2, …) drives the legacy client statement; date-gate with CASE WHEN TradeDate >= '2027-10-11'.  
  owners: @ardencm/settlements-legacy-db

### platform-terraform
- `batch/schedules.tf` L4 — EventBridge cron for instruction generation moves to T evening to match the JIL change.  
  owners: @ardencm/platform-eng

### recon-job
- `recon/expected.py` L6 — DEFAULT_LAG 2→ date-gated 1; LATE_MATCH_TOLERANCE_DAYS of 1 becomes 100% of the cycle — recon ops to decide whether 'late match' survives.  
  owners: @ardencm/ops-recon

### risk-lib
- `risk_lib/settlement_exposure.py` L11 — EU/UK/CH lag 2→1 from go-live (region-level; JP stays 2). Days-at-risk and exposure figures halve — risk to re-baseline limits.  
  owners: @ardencm/risk-quant, @ardencm/settlements-core

## Tranche 3 — Business decisions required before code changes

Not coded. Each item is routed to a named human owner with the question written out; engineering re-enters once a decision exists.

### core-dates
- `docs/t1-calendar-mismatch.md` L3 — Venue vs TARGET2 calendar divergence under T+1 (Whit Monday, UK bank holidays, Boxing Day substitute). Which calendar is authoritative is not a code decision.  
  owners: @ardencm/post-trade-core · **decision: @ardencm/market-ops + Head of Post-Trade**
- `src/main/resources/holidays.yaml` L6 — Calendar data is correct; the *rule* for divergent dates is the open question above.  
  owners: @ardencm/post-trade-core, @ardencm/market-ops · **decision: @ardencm/market-ops**

### corporate-actions-service
- `docs/ex-date-policy.md` L7 — Policy document records the open question.  
  owners: @ardencm/asset-servicing · **decision: asset-servicing ops (CA-2291)**
- `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java` L12 — Ex-date = record date under T+1, but CA-2291 is open: events announced before go-live with record dates after it, and dual-listed XSWX/XLON names. Do not change until asset-servicing ops rules.  
  owners: @ardencm/asset-servicing · **decision: asset-servicing ops (CA-2291)**

### fx-funding-service
- `README.md` L10 — Documents the convention above.  
  owners: @ardencm/treasury-tech · **decision: treasury**
- `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java` L12 — FX spot stays T+2 (market convention, not CSDR). Under T+1 every cross-currency trade needs tom-next or pre-funding — treasury policy, not a code fix.  
  owners: @ardencm/treasury-tech · **decision: treasury**

### legacy-position-keeper
- `src/com/arden/poskeeper/SettleDateUtil.java` L20 — SETTLE_DAYS = 2 in a JDK 8 Ant build that cannot take core-dates (PK-118); original author left 2018. Needs an owner and a decision: patch the constant with a date gate, or retire the service.  
  owners: @ardencm/settlements-legacy · **decision: @ardencm/legacy-platform + Head of Post-Trade**

### stock-loan-recall-service
- `README.md` L10 — States the notice period is a legal-agreement change.  
  owners: @ardencm/sec-lending · **decision: securities lending + legal**
- `src/main/java/com/ardencm/seclending/recall/RecallService.java` L13 — GMSLA 2-business-day recall notice makes the deadline fall *before* trade date under T+1. Notice period is contractual — legal/seclending to renegotiate or accept fails.  
  owners: @ardencm/sec-lending · **decision: securities lending + legal**

## Open decisions (human-owned)

- **@ardencm/legacy-platform + Head of Post-Trade** — legacy-position-keeper/SettleDateUtil.java
- **@ardencm/market-ops** — core-dates/holidays.yaml
- **@ardencm/market-ops + Head of Post-Trade** — core-dates/t1-calendar-mismatch.md
- **asset-servicing ops (CA-2291)** — corporate-actions-service/ExDateCalculator.java, corporate-actions-service/ex-date-policy.md
- **securities lending + legal** — stock-loan-recall-service/README.md, stock-loan-recall-service/RecallService.java
- **treasury** — fx-funding-service/FxFundingService.java, fx-funding-service/README.md

## Verification

- Golden file `demo/t1/golden/trades-2027Q4.csv` replayed through the pre- and post-migration `core-dates` CLI; every difference is listed and classified (expected / unchanged / escalate).
- Repo unit tests and `*ContractTest` per PR (tag 64 stays `yyyyMMdd`; tag 63 moves 3→2 only for post-go-live EU/UK/CH trades).
- Calendar-divergence trades are reported, not resolved (see `core-dates/docs/t1-calendar-mismatch.md`).
