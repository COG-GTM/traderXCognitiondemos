# T+1 migration — tranche 1 report (SETTLE-4471)

Generated 2026-10-23T03:10:55Z. Scope: EU, UK, Switzerland regular-way settlement T+2 → T+1 on **11 October 2027**. Synthetic estate (Arden Capital Markets, 20 repos).

## 1. What was found

- 57 files in 17 repositories carry a settlement-cycle assumption (105 line citations, `demo2-t1-inventory.md`).
- Tranche split: 18 files in the instruction path (tranche 1), 21 downstream/scheduler/wording (tranche 2), 16 that need a business decision first (tranche 3).
- Decisions owed: @ardencm/market-ops + Head of Post-Trade, Asset-servicing ops (CA-2291), Head of Post-Trade (London position roll system owner, PK-118), Securities-lending desk head + Legal (GMSLA schedules), Treasury funding desk (Head of Treasury).

## 2. What was changed (tranche 1)

### core-dates — `migrated-with-deviation` · branch `devin/settle-4471-t1-core-dates` · reviewers @ardencm/post-trade-core
1. FAIL — cut 1.5.1 from core-dates main (1.5.0) — own tests 10/10 green, downstream custody contract run against it  
   `settlement-instruction-service vs core-dates 1.5.1: Tests run: 5, Failures: 4 — CustodyGatewayContractTest.tag64IsLocalMktDate: tag 64 must match ^[0-9]{8}$ but was 2027-10-13`
   - cause: 1.5.0 changed FixDates LOCAL_MKT_DATE to ISO_LOCAL_DATE (yyyy-MM-dd) — an unrelated wire-format change; consumers run 1.4.0 (common-java-bom pin)
2. PASS — cut 1.4.2 from release/1.4 (the line consumers run) — same change  
   `core-dates: Tests run: 10, Failures: 0; settlement-instruction-service + CustodyGatewayContractTest vs 1.4.2: Tests run: 5, Failures: 0`
   - smallest deviation keeping the custody contract green; contract test untouched; BOM pin not moved
   PR artifact: `demo/out/t1/prs/core-dates/PR.md`

### settlement-instruction-service — `migrated-with-deviation` · branch `devin/settle-4471-t1-settlement-instruction-service` · reviewers @ardencm/settlements-core, @ardencm/market-ops
1. FAIL — build against core-dates 1.5.1 (from main)  
   `Tests run: 5, Failures: 4 (tag 64/75 rendered yyyy-MM-dd)`
   - see core-dates attempt 1
2. PASS — drop EU/UK/CH T_PLUS_2 pins, cycleFor(mic, tradeDate), consume core-dates 1.4.2 via a service-local version (BOM pin unchanged)  
   `Tests run: 5, Failures: 0, Errors: 0 (incl. CustodyGatewayContractTest)`
   PR artifact: `demo/out/t1/prs/settlement-instruction-service/PR.md`

### iso20022-fix-messages — `migrated` · branch `devin/settle-4471-t1-iso20022-fix-messages` · reviewers @ardencm/messaging-standards, @ardencm/settlements-core
1. PASS — effective_from 2027-10-11 mapping, archived pre-migration mapping + golden samples, validator selects by tag 75  
   `python -m pytest: 7 passed; validate.py: 3 samples ok`
   PR artifact: `demo/out/t1/prs/iso20022-fix-messages/PR.md`

## 3. Parity — what the counterparty would see

Golden file `demo/t1/golden/trades-2027Q4.csv` (30 trades) through core-dates baseline-cli.jar vs 1.4.2: **20 changed**, 10 unchanged, 16 expected, **4 escalated**.

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

## 6. Frontend check (phase 4)

- Recording: `arden-phase4-edited.mp4` (attached to the Devin session and PR #40 comment).
- `client-portal` in the workspace is backend-only (`/api/portfolio/valuation`, `/api/settlement/summary`, `/health` → 200; `/trade-blotter`, `/settlement-status` → 404). The UI lives in `client-portal-ui`, which is not in the estate, so loading a pre- and post-go-live trade in the blotter was **not verified**. The Node server does not call core-dates.
- Stale wording (tranche 2, left unchanged): `/api/settlement/summary` → "Trades settle 2 business days after execution (T+2). US equities settle T+1." — owner @ardencm/client-digital; listed in `demo2-t1-migration-plan.md`.
