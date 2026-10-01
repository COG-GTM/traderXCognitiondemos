# T+1 settlement-cycle inventory — SETTLE-4471 (EU/UK/CH T+1, go-live 11 Oct 2027)

Generated 2026-09-25T21:19:32Z by `demo/t1/inventory.py` over 20 repositories in the Arden Capital Markets estate (synthetic). 37 files in 16 repos carry a settlement-cycle assumption; 64 line citations. Every row below is a claim a reviewer can check by opening the file.

| Repo | File | Line(s) | What | Owners (CODEOWNERS) | Tranche |
|---|---|---|---|---|---|
| core-dates | `src/main/java/com/ardencm/posttrade/dates/FixDates.java` | 26, 34 | cycle literal | @ardencm/post-trade-core | 1 |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java` | 10, 16, 34, 43 | cycle literal | @ardencm/post-trade-core | 1 |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java` | 11 | cycle literal | @ardencm/post-trade-core | 1 |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java` | — | policy/doc | @ardencm/post-trade-core | 1 |
| core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java` | 21, 64, 65 | cycle literal | @ardencm/post-trade-core | 1 |
| iso20022-fix-messages | `README.md` | — | policy/doc | @ardencm/messaging-standards | 1 |
| iso20022-fix-messages | `mapping/archive/settlement-cycle-pre-t1.yaml` | — | policy/doc | @ardencm/messaging-standards, @ardencm/settlements-core | 1 |
| iso20022-fix-messages | `mapping/settlement-cycle.yaml` | 2, 3, 4, 9, 10, 11, 12 | cycle literal, wire mapping | @ardencm/messaging-standards, @ardencm/settlements-core | 1 |
| settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java` | — | policy/doc | @ardencm/settlements-core | 1 |
| settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java` | — | policy/doc | @ardencm/settlements-core | 1 |
| settlement-instruction-service | `src/main/resources/application.yml` | 11, 12, 13, 14, 15 | cycle literal | @ardencm/settlements-core, @ardencm/market-ops | 1 |
| settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java` | 31 | cycle literal | @ardencm/settlements-core | 1 |
| settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java` | 15 | cycle literal | @ardencm/settlements-core | 1 |
| allocation-service | `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java` | 17 | hard-coded lag | @ardencm/post-trade-allocations | 2 |
| allocation-service | `src/main/resources/application.yml` | 8 | cycle literal | @ardencm/post-trade-allocations, @ardencm/market-ops | 2 |
| batch-scheduler-config | `calendars/settlement-cutoffs.yaml` | 3, 4, 5, 6 | cycle literal | @ardencm/batch-ops | 2 |
| batch-scheduler-config | `jil/settlements.jil` | 18, 27, 36 | cycle literal | @ardencm/batch-ops, @ardencm/settlements-core | 2 |
| client-portal | `src/server.js` | 18 | cycle literal | @ardencm/client-digital | 2 |
| confirmation-service | `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java` | 23, 24 | cycle literal | @ardencm/post-trade-confirms | 2 |
| confirmation-service | `src/main/resources/application.yml` | 7, 9 | cycle literal, prose assumption | @ardencm/post-trade-confirms, @ardencm/market-ops | 2 |
| confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java` | 12 | cycle literal | @ardencm/post-trade-confirms | 2 |
| custody-gateway | `src/main/resources/application.yml` | 7 | cycle literal | @ardencm/custody-integration, @ardencm/market-ops | 2 |
| legacy-stored-procs | `README.md` | 5 | hard-coded lag | @ardencm/settlements-legacy-db | 2 |
| legacy-stored-procs | `procs/usp_FlagLateSettlements.sql` | 4, 8 | cycle literal, hard-coded lag | @ardencm/settlements-legacy-db | 2 |
| legacy-stored-procs | `views/vw_SettlementCalendar.sql` | 4 | hard-coded lag | @ardencm/settlements-legacy-db | 2 |
| platform-terraform | `batch/schedules.tf` | 4 | cycle literal | @ardencm/platform-eng | 2 |
| recon-job | `recon/expected.py` | 6, 8 | cycle literal, hard-coded lag | @ardencm/ops-recon | 2 |
| risk-lib | `risk_lib/settlement_exposure.py` | 11, 12, 13, 14 | hard-coded lag | @ardencm/risk-quant, @ardencm/settlements-core | 2 |
| core-dates | `docs/t1-calendar-mismatch.md` | 3 | cycle literal | @ardencm/post-trade-core | 3 |
| core-dates | `src/main/resources/holidays.yaml` | 6 | cycle literal | @ardencm/post-trade-core, @ardencm/market-ops | 3 |
| corporate-actions-service | `docs/ex-date-policy.md` | 7 | cycle literal | @ardencm/asset-servicing | 3 |
| corporate-actions-service | `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java` | 12, 15 | cycle literal, hard-coded lag | @ardencm/asset-servicing | 3 |
| fx-funding-service | `README.md` | 10 | cycle literal | @ardencm/treasury-tech | 3 |
| fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java` | 12, 13 | cycle literal, hard-coded lag | @ardencm/treasury-tech | 3 |
| legacy-position-keeper | `src/com/arden/poskeeper/SettleDateUtil.java` | 20 | hard-coded lag | @ardencm/settlements-legacy | 3 |
| stock-loan-recall-service | `README.md` | 10 | prose assumption | @ardencm/sec-lending | 3 |
| stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java` | 13, 14, 16 | cycle literal, hard-coded lag, prose assumption | @ardencm/sec-lending | 3 |

## Citations

### allocation-service/src/main/java/com/ardencm/posttrade/allocation/AllocationService.java
- L17 (hard-coded lag): `private static final int GIVE_UP_SETTLEMENT_DAYS = 2;`

### allocation-service/src/main/resources/application.yml
- L8 (cycle literal): `default-cycle: T+2`

### batch-scheduler-config/calendars/settlement-cutoffs.yaml
- L3 (cycle literal): `XLON: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }`
- L4 (cycle literal): `XPAR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }`
- L5 (cycle literal): `XETR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }`
- L6 (cycle literal): `XSWX: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }`

### batch-scheduler-config/jil/settlements.jil
- L18 (cycle literal): `description: "Generate settlement instructions for trades dated T-1; instructions must reach CSD before T+1 12:00 CET match cut-off for T+2 `
- L27 (cycle literal): `description: "Chase clients for unaffirmed trades; deadline is 12:00 on T+1 (T+2 cycle)"`
- L36 (cycle literal): `description: "Issue recalls for sales dated today so shares return before S (T+2)"`

### client-portal/src/server.js
- L18 (cycle literal): `res.end(JSON.stringify({ message: 'Trades settle 2 business days after execution (T+2). US equities settle T+1. Amounts shown in the account`

### confirmation-service/src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java
- L23 (cycle literal): `// All non-US confirms are regular-way T+2; US moved in May 2024.`
- L24 (cycle literal): `SettlementCycle cycle = mic.startsWith("XN") ? SettlementCycle.T_PLUS_1 : SettlementCycle.T_PLUS_2;`

### confirmation-service/src/main/resources/application.yml
- L7 (cycle literal): `narrative: "This trade will settle on a T+2 basis. Expected settlement date: {settlement}. Please affirm by 12:00 local time on T+1."`
- L9 (prose assumption): `footer: "Regular-way settlement is two business days after trade date unless otherwise stated."`

### confirmation-service/src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java
- L12 (cycle literal): `new ConfirmationService("Settles {settlement} on a T+2 basis.");`

### core-dates/docs/t1-calendar-mismatch.md
- L3 (cycle literal): `Under T+2, a trading-venue closure that is not a TARGET2 closure (or vice versa) rarely changed`

### core-dates/src/main/java/com/ardencm/posttrade/dates/FixDates.java
- L26 (cycle literal): `* '3' = T+2, '4' = T+3.`
- L34 (cycle literal): `case T_PLUS_2:`

### core-dates/src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java
- L10 (cycle literal): `* trades dated before go-live settle T+2, trades dated on or after it settle T+1. Callers must pass the trade`
- L16 (cycle literal): `T_PLUS_2(2),`
- L34 (cycle literal): `return tradeDate.isBefore(EU_UK_CH_T1_GO_LIVE) ? T_PLUS_2 : T_PLUS_1;`
- L43 (cycle literal): `return T_PLUS_2;`

### core-dates/src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java
- L11 (cycle literal): `* java -jar core-dates.jar [--cycle T_PLUS_1|T_PLUS_2] &lt; trades.csv`

### core-dates/src/main/resources/holidays.yaml
- L6 (cycle literal): `# but XLON-open). Under T+2 these mismatches were absorbed by the extra day; under`

### core-dates/src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java
- L21 (cycle literal): `// Fri 8 Oct 2027 (T+2) and Mon 11 Oct 2027 (T+1) both settle Tue 12 Oct: the double-settlement day.`
- L64 (cycle literal): `assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.standard());`
- L65 (cycle literal): `assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XLON"));`

### corporate-actions-service/docs/ex-date-policy.md
- L7 (cycle literal): `| T+2 (EU/UK/CH today) | record date − 1 business day |`

### corporate-actions-service/src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java
- L12 (cycle literal): `* Ex-date = record date minus (settlement cycle - 1) business days. Under T+2 that is one business day`
- L15 (hard-coded lag): `private static final int EX_DATE_OFFSET_BUSINESS_DAYS = 1;`

### custody-gateway/src/main/resources/application.yml
- L7 (cycle literal): `# Instructions must reach the CSD before the T+1 12:00 CET matching cut-off for T+2 settlement.`

### fx-funding-service/README.md
- L10 (cycle literal): `FX spot value date follows FX market convention (T+2), which is independent of securities settlement cycle. When securities settle before sp`

### fx-funding-service/src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java
- L12 (cycle literal): `/** FX spot convention: value date is T+2 for most pairs. This is an FX market convention, not CSDR. */`
- L13 (hard-coded lag): `private static final int FX_SPOT_DAYS = 2;`

### iso20022-fix-messages/mapping/settlement-cycle.yaml
- L2 (cycle literal): `# FIX tag 63 (SettlType): 0=Regular, 1=Cash(T+0), 2=NextDay(T+1), 3=T+2, 4=T+3`
- L3 (cycle literal): `default_cycle: T+2`
- L4 (wire mapping): `default_fix_settl_type: "3"`
- L9 (cycle literal): `XLON: { cycle: T+2, fix_settl_type: "3" }`
- L10 (cycle literal): `XPAR: { cycle: T+2, fix_settl_type: "3" }`
- L11 (cycle literal): `XETR: { cycle: T+2, fix_settl_type: "3" }`
- L12 (cycle literal): `XSWX: { cycle: T+2, fix_settl_type: "3" }`

### legacy-position-keeper/src/com/arden/poskeeper/SettleDateUtil.java
- L20 (hard-coded lag): `public static final int SETTLE_DAYS = 2;`

### legacy-stored-procs/README.md
- L5 (hard-coded lag): ``usp_FlagLateSettlements` (hard-coded `DATEADD(day, 2, …)`) and `vw_SettlementCalendar` (hard-coded, statement only).`

### legacy-stored-procs/procs/usp_FlagLateSettlements.sql
- L4 (cycle literal): `-- A trade is late if not settled by close of T+2 (regular way). Fed to the CSDR penalty report.`
- L8 (hard-coded lag): `AND DATEADD(day, 2, t.TradeDate) < CAST(GETDATE() AS DATE);`

### legacy-stored-procs/views/vw_SettlementCalendar.sql
- L4 (hard-coded lag): `DATEADD(day, 2, t.TradeDate) AS NaiveSettlementDate,   -- used by the legacy client statement only`

### platform-terraform/batch/schedules.tf
- L4 (cycle literal): `schedule_expression = "cron(0 6 ? * MON-FRI *)" # 06:00 London; instructions for T-1 trades, T+2 cycle`

### recon-job/recon/expected.py
- L6 (cycle literal): `# Venue -> business-day lag. Anything missing is assumed regular-way T+2.`
- L8 (hard-coded lag): `DEFAULT_LAG = 2`

### risk-lib/risk_lib/settlement_exposure.py
- L11 (hard-coded lag): `"EU": 2,`
- L12 (hard-coded lag): `"UK": 2,`
- L13 (hard-coded lag): `"CH": 2,`
- L14 (hard-coded lag): `"JP": 2,`

### settlement-instruction-service/src/main/resources/application.yml
- L11 (cycle literal): `# EU/UK/CH venues: regular way T+2 (CSDR art. 5). Migration to T+1 scheduled 11 Oct 2027 — see SETTLE-4471.`
- L12 (cycle literal): `XLON: T_PLUS_2`
- L13 (cycle literal): `XPAR: T_PLUS_2`
- L14 (cycle literal): `XETR: T_PLUS_2`
- L15 (cycle literal): `XSWX: T_PLUS_2`

### settlement-instruction-service/src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java
- L31 (cycle literal): `cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));`

### settlement-instruction-service/src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java
- L15 (cycle literal): `cfg.setCycles(Map.of("XNYS", SettlementCycle.T_PLUS_1, "XLON", SettlementCycle.T_PLUS_2));`

### stock-loan-recall-service/README.md
- L10 (prose assumption): `Recall notice period comes from GMSLA schedules (2 business days). Any change is a legal-agreement change, not just a config change.`

### stock-loan-recall-service/src/main/java/com/ardencm/seclending/recall/RecallService.java
- L13 (prose assumption): `* Standard recall notice period agreed in our GMSLA schedules: borrower returns within 2 business days.`
- L14 (cycle literal): `* Under T+2 a recall issued on trade date lands on settlement date; there is no slack.`
- L16 (hard-coded lag): `static final int RECALL_NOTICE_BUSINESS_DAYS = 2;`
