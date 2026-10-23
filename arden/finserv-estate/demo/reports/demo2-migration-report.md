# T+1 migration — tranche 1 report (SETTLE-4471)

Generated 2026-10-23T03:00:37Z. Scope: EU, UK, Switzerland regular-way settlement T+2 → T+1 on **11 October 2027**. Synthetic estate (Arden Capital Markets, 20 repos).

## 1. What was found

- See the inventory produced for this ticket (`demo/out/t1/inventory.json` not present).

## 2. What was changed (tranche 1)

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
