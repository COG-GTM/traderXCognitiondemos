# T+1 migration — tranche 1 report (SETTLE-4471)

Generated 2026-10-26T01:36:28Z. Scope: EU, UK, Switzerland regular-way settlement T+2 → T+1 on **11 October 2027**. Synthetic estate (Arden Capital Markets, 20 repos).

## 1. What was found

- 52 files in 16 repositories carry a settlement-cycle assumption (124 line citations, `demo2-t1-inventory.md`).
- Tranche split: 14 files in the instruction path (tranche 1), 23 downstream/scheduler/wording (tranche 2), 12 that need a business decision first (tranche 3).
- Decisions owed: @ardencm/market-ops + Head of Post-Trade, Asset-servicing ops, CA-2291, Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending, Treasury (FX funding), via @ardencm/treasury-tech.

## 2. What was changed (tranche 1)

### core-dates — `migrated-with-deviation` · branch `devin/1792978494-settle-4471-t1-tranche1-core-dates` · reviewers @ardencm/post-trade-core
1. FAIL — build tranche 1 on core-dates main (1.5.0 line) as 1.6.0-SNAPSHOT, run settlement-instruction-service consumer contract  
   `CustodyGatewayContractTest.tag64IsLocalMktDate: tag 64 must match ^[0-9]{8}$ but was 2027-10-13`
   - main carries the 1.5.0 FixDates ISO-8601 change (unrelated to T+1); contract not weakened
2. PASS — cut 1.4.2 from release/1.4 (line consumers run via BOM 1.4.0 / patch 1.4.1)  
   `core-dates 10/10; settlement-instruction-service 6/6 incl. CustodyGatewayContractTest against 1.4.2`
   PR artifact: `demo/out/t1/prs/core-dates/PR.md`

### settlement-instruction-service — `migrated` · branch `devin/1792978494-settle-4471-t1-tranche1-settlement-instruction-service` · reviewers @ardencm/settlements-core, @ardencm/market-ops
1. PASS — cycleFor(mic, tradeDate); drop EU/UK/CH T_PLUS_2 pins; core-dates 1.4.2 override in service pom (BOM unchanged)  
   `mvn -q verify: InstructionBuilderTest 5/5, CustodyGatewayContractTest 1/1`
   PR artifact: `demo/out/t1/prs/settlement-instruction-service/PR.md`

### iso20022-fix-messages — `migrated` · branch `devin/1792978494-settle-4471-t1-tranche1-iso20022-fix-messages` · reviewers @ardencm/messaging-standards, @ardencm/settlements-core
1. PASS — mapping effective_from 2027-10-11 with before: T+2; validator selects by tag 75; archive pre-migration mapping and samples  
   `python -m pytest: 9 passed; python validate.py: 2 samples ok`
   PR artifact: `demo/out/t1/prs/iso20022-fix-messages/PR.md`

## 3. Parity — what the counterparty would see

Golden file `demo/t1/golden/trades-2027Q4.csv` (30 trades) through core-dates 1.4.0 vs 1.4.2: **20 changed**, 10 unchanged, 16 expected, **4 escalated**.

| trade | date | MIC | settle old → new | tag 63 | affirm-by old → new | verdict | why |
|---|---|---|---|---|---|---|---|
| ARD-27Q4-0001 | 2027-10-06 | XLON | 2027-10-08 | 3 | 2027-10-07 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0002 | 2027-10-07 | XLON | 2027-10-11 | 3 | 2027-10-08 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0003 | 2027-10-08 | XLON | 2027-10-12 | 3 | 2027-10-11 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0004 | 2027-10-08 | XPAR | 2027-10-12 | 3 | 2027-10-11 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0005 | 2027-10-08 | XETR | 2027-10-12 | 3 | 2027-10-11 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0006 | 2027-10-08 | XSWX | 2027-10-12 | 3 | 2027-10-11 | unchanged | trade date before go-live: T+2 preserved (date gate) |
| ARD-27Q4-0007 | 2027-10-08 | XNYS | 2027-10-11 | 2 | 2027-10-08 | unchanged | already T+1 (since 28 May 2024) |
| ARD-27Q4-0008 | 2027-10-11 | XLON | 2027-10-13 → **2027-10-12** | 3 → **2** | 2027-10-12 → **2027-10-11** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · double-settlement day: settles alongside the last T+2 trades of 8 Oct; affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0009 | 2027-10-11 | XPAR | 2027-10-13 → **2027-10-12** | 3 → **2** | 2027-10-12 → **2027-10-11** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · double-settlement day: settles alongside the last T+2 trades of 8 Oct; affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0010 | 2027-10-11 | XETR | 2027-10-13 → **2027-10-12** | 3 → **2** | 2027-10-12 → **2027-10-11** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · double-settlement day: settles alongside the last T+2 trades of 8 Oct; affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0011 | 2027-10-11 | XSWX | 2027-10-13 → **2027-10-12** | 3 → **2** | 2027-10-12 → **2027-10-11** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · double-settlement day: settles alongside the last T+2 trades of 8 Oct; affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0012 | 2027-10-11 | XNYS | 2027-10-12 | 2 | 2027-10-11 | unchanged | already T+1 (since 28 May 2024) |
| ARD-27Q4-0013 | 2027-10-11 | XTSE | 2027-10-13 | 2 | 2027-10-12 | unchanged | already T+1 (since 28 May 2024) |
| ARD-27Q4-0014 | 2027-10-12 | XLON | 2027-10-14 → **2027-10-13** | 3 → **2** | 2027-10-13 → **2027-10-12** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0015 | 2027-10-14 | XLON | 2027-10-18 → **2027-10-15** | 3 → **2** | 2027-10-15 → **2027-10-14** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0016 | 2027-10-15 | XLON | 2027-10-19 → **2027-10-18** | 3 → **2** | 2027-10-18 → **2027-10-15** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0017 | 2027-10-15 | XETR | 2027-10-19 → **2027-10-18** | 3 → **2** | 2027-10-18 → **2027-10-15** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0018 | 2027-11-24 | XNYS | 2027-11-26 | 2 | 2027-11-24 | unchanged | already T+1 (since 28 May 2024) |
| ARD-27Q4-0019 | 2027-12-22 | XLON | 2027-12-24 → **2027-12-23** | 3 → **2** | 2027-12-23 → **2027-12-22** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0020 | 2027-12-23 | XLON | 2027-12-29 → **2027-12-24** | 3 → **2** | 2027-12-24 → **2027-12-23** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0021 | 2027-12-23 | XETR | 2027-12-28 → **2027-12-27** | 3 → **2** | 2027-12-27 → **2027-12-23** | ESCALATE | venue (XETR) settles 2027-12-27 but cash calendar (TARGET2) says 2027-12-24 — which is authoritative is a business decision → @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md) |
| ARD-27Q4-0022 | 2027-12-23 | XPAR | 2027-12-29 → **2027-12-28** | 3 → **2** | 2027-12-28 → **2027-12-23** | ESCALATE | venue (XPAR) settles 2027-12-28 but cash calendar (TARGET2) says 2027-12-24 — which is authoritative is a business decision → @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md) |
| ARD-27Q4-0023 | 2027-12-23 | XSWX | 2027-12-28 → **2027-12-27** | 3 → **2** | 2027-12-27 → **2027-12-23** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0024 | 2027-12-24 | XLON | 2027-12-30 → **2027-12-29** | 3 → **2** | 2027-12-29 → **2027-12-24** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0025 | 2027-12-29 | XLON | 2027-12-31 → **2027-12-30** | 3 → **2** | 2027-12-30 → **2027-12-29** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0026 | 2027-12-30 | XLON | 2028-01-03 → **2027-12-31** | 3 → **2** | 2027-12-31 → **2027-12-30** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0027 | 2027-12-30 | XPAR | 2028-01-04 → **2028-01-03** | 3 → **2** | 2028-01-03 → **2027-12-30** | ESCALATE | venue (XPAR) settles 2028-01-03 but cash calendar (TARGET2) says 2027-12-31 — which is authoritative is a business decision → @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md) |
| ARD-27Q4-0028 | 2027-12-30 | XETR | 2028-01-04 → **2028-01-03** | 3 → **2** | 2028-01-03 → **2027-12-30** | ESCALATE | venue (XETR) settles 2028-01-03 but cash calendar (TARGET2) says 2027-12-31 — which is authoritative is a business decision → @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md) |
| ARD-27Q4-0029 | 2027-12-30 | XSWX | 2028-01-04 → **2028-01-03** | 3 → **2** | 2028-01-03 → **2027-12-30** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |
| ARD-27Q4-0030 | 2027-12-31 | XLON | 2028-01-04 → **2028-01-03** | 3 → **2** | 2028-01-03 → **2027-12-31** | expected | T+2 → T+1; tag 63 3→2; tag 64 format unchanged · affirmation deadline collapses onto trade date (tranche 2: cut-offs) |

## 4. Escalations — decisions the code does not make

- **ARD-27Q4-0021** (2027-12-23 XETR): venue (XETR) settles 2027-12-27 but cash calendar (TARGET2) says 2027-12-24 — which is authoritative is a business decision. Candidates: venue 2027-12-27 / cash 2027-12-24. Owner: @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md)
- **ARD-27Q4-0022** (2027-12-23 XPAR): venue (XPAR) settles 2027-12-28 but cash calendar (TARGET2) says 2027-12-24 — which is authoritative is a business decision. Candidates: venue 2027-12-28 / cash 2027-12-24. Owner: @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md)
- **ARD-27Q4-0027** (2027-12-30 XPAR): venue (XPAR) settles 2028-01-03 but cash calendar (TARGET2) says 2027-12-31 — which is authoritative is a business decision. Candidates: venue 2028-01-03 / cash 2027-12-31. Owner: @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md)
- **ARD-27Q4-0028** (2027-12-30 XETR): venue (XETR) settles 2028-01-03 but cash calendar (TARGET2) says 2027-12-31 — which is authoritative is a business decision. Candidates: venue 2028-01-03 / cash 2027-12-31. Owner: @ardencm/market-ops + Head of Post-Trade (core-dates/docs/t1-calendar-mismatch.md)
- **CA-27Q4-0101** (GB0002634946, record 2027-10-22): announced before go-live, record date after: policy does not say which ex-date rule applies. Ex-date would be 2027-10-21 (T+2 rule) or 2027-10-22 (T+1 rule). Owner: asset-servicing ops, CA-2291 (corporate-actions-service/docs/ex-date-policy.md)
- **CA-27Q4-0102** (CH0012032048, record 2027-10-15): announced before go-live, record date after: policy does not say which ex-date rule applies. Ex-date would be 2027-10-14 (T+2 rule) or 2027-10-15 (T+1 rule). Owner: asset-servicing ops, CA-2291 (corporate-actions-service/docs/ex-date-policy.md)

## 5. Not done, on purpose

- Tranche 2 (schedulers, cut-offs, recon, risk, client wording, legacy SQL) is planned per repo in `demo2-t1-migration-plan.md`, not executed.
- Tranche 3 items (calendar authority, ex-date regime, legacy position keeper, FX funding, GMSLA recall notice) are escalated above and left unchanged.
- No branch is merged; no BOM pin moved. `core-dates` main still carries the 1.5.0 wire change and needs its own decision before tranche 1 can be forward-ported.

## 6. Every changed parity row, explained

All 20 changed rows are in-scope trades (XLON, XPAR, XETR, XSWX) dated on or after 2027-10-11. Each moves one business day earlier (T+2 to T+1), tag 63 goes `3` to `2`, tag 64 keeps the `yyyyMMdd` format (8 digits, old and new), and the affirmation deadline moves to the trade date. The 10 unchanged rows are pre-go-live in-scope trades (T+2 kept by the date gate) or XNYS/XTSE trades (already T+1).

- **16 expected:** 0008-0011 (go-live day, settle Tue 2027-10-12 together with the last T+2 trades of Fri 2027-10-08), 0014-0017, 0019, 0020, 0023, 0024-0026, 0029, 0030. Year-end rows skip the holidays of the venue calendar, e.g. 0020 XLON 2027-12-23 settles 2027-12-24 (was 2027-12-29), 0026 XLON 2027-12-30 settles 2027-12-31 (was 2028-01-03).
- **4 ESCALATE (E1, calendar authority):** the venue and TARGET2 calendars disagree, so the T+1 date depends on which is authoritative. The code keeps the venue calendar and decides nothing. 0021 XETR venue 2027-12-27 / cash 2027-12-24; 0022 XPAR 2027-12-28 / 2027-12-24; 0027 XPAR 2028-01-03 / 2027-12-31; 0028 XETR 2028-01-03 / 2027-12-31. Owner: @ardencm/market-ops + Head of Post-Trade.
- **Corporate actions (E2):** CA-27Q4-0101 XLON ex-date 2027-10-21 (T+2 rule) or 2027-10-22 (T+1 rule); CA-27Q4-0102 XSWX/XLON dual listing 2027-10-14 or 2027-10-15. CA-27Q4-0103 is post-go-live and pending the policy; CA-27Q4-0104 is unchanged. Owner: asset-servicing ops, CA-2291. `ExDateCalculator` not changed.

## 7. Contract and consumer checks

- custody-gateway consumer contract (`CustodyGatewayContractTest`, tag 64 `^[0-9]{8}$`): **FAIL** against tranche 1 built on core-dates `main` (`tag 64 must match ^[0-9]{8}$ but was 2027-10-13`, 1.5.0 ISO change), **PASS** against 1.4.2 cut from `release/1.4`. Contract test unchanged.
- Other core-dates consumers run against 1.4.2 with the BOM left at 1.4.0 (for information, not changed): confirmation-service, allocation-service, fx-funding-service and stock-loan-recall-service each fail one T+2-pinned test (`expected 2027-10-13 but was 2027-10-12`); corporate-actions-service passes. So the BOM move waits for tranche 2 and for E3/E4.

## 8. Escalations (one per decision owner)

| # | decision | owner |
|---|---|---|
| E1 | venue vs cash calendar for T+1 settlement dates (4 golden rows) | @ardencm/market-ops + Head of Post-Trade |
| E2 | ex-date rule for events straddling go-live, dual listings (CA-27Q4-0101, -0102) | Asset-servicing ops, CA-2291 |
| E3 | FX funding with spot T+2 and securities T+1 | Treasury (FX funding), via @ardencm/treasury-tech |
| E4 | GMSLA recall notice period (2 business days) under T+1 | Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending |

Posted to each PR and listed in `demo2-t1-migration-plan.md`; not posted to Slack (instructed).
