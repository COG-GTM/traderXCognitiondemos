# SETTLE-4471 T+1 inventory (programme T1-2027)

Scope as given: EU, UK and CH regular-way trades, T+2 -> T+1, go-live **2027-10-11**. Estate: Arden, 20 repos in `~/finserv-workspace` (seeded by `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh`).

**52 files, 124 line citations, 16 of 20 repos.** Files by tranche: T1 14 · T2 23 · T3 (business decision) 12 · no change 3. Unassessed: 0.

Tranche 1 = instruction-generation path (core-dates, settlement-instruction-service, iso20022-fix-messages). Tranche 2 = downstream consumers, schedules, wording. Tranche 3 = needs a named business decision first.

Repos scanned with no settlement-cycle hit: ci-shared-workflows, common-java-bom, eod-pricing-batch, market-data-feed-contracts.

Method: regex scan over every tracked file (patterns in `inventory.py`: `T_PLUS_2`, `T+2`, lag constants, `plusDays(2)`, `DATEADD(day, 2`, tag 63 = 3, `default_cycle`, cut-off/scheduler times keyed to T+1, FX spot, recall notice, ex-date rules, two-arg `SettlementDates.settlementDate` callers that inherit the market default, test pins of T+2 dates). Owners = last matching CODEOWNERS rule.

## Files

| repo | file | kind | lines | CODEOWNERS | tranche | assessment | decision owner |
|---|---|---|---|---|---|---|---|
| allocation-service | `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java` | hard-coded lag | 17, 21 | @ardencm/post-trade-allocations | 2 | Give-ups use tradeDate.plusDays(2) calendar days (ops request 2019) and ignore holidays/cycle; replace with core-dates | - |
| allocation-service | `src/main/resources/application.yml` | mapping | 8 | @ardencm/post-trade-allocations @ardencm/market-ops | 2 | default-cycle: T+2 for client confirms; date-gate with post-trade ops | - |
| allocation-service | `src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java` | test | 16, 21 | @ardencm/post-trade-allocations | 2 | Test pins T+2 dates; update with the service change | - |
| batch-scheduler-config | `calendars/settlement-cutoffs.yaml` | mapping | 3, 4, 5, 6 | @ardencm/batch-ops | 2 | EU/UK/CH cut-offs 12:00 T+1, cycle T+2 | - |
| batch-scheduler-config | `jil/settlements.jil` | mapping, schedule/cut-off | 15, 18, 25, 27, 34, 36 | @ardencm/batch-ops @ardencm/settlements-core | 2 | SETTLE_INSTR_GEN 06:00 next morning and AFFIRMATION_CHASER 09:00 T+1 assume T+2; must move to trade date evening. RECALL_SWEEP wording | - |
| client-portal | `src/server.js` | mapping | 18 | @ardencm/client-digital | 2 | /api/settlement/summary says 'Trades settle 2 business days after execution (T+2)' | - |
| confirmation-service | `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java` | hard-coded lag, mapping, market-default caller | 21, 23, 24 | @ardencm/post-trade-confirms | 2 | tag 63 hard-coded T_PLUS_2 for non-XN venues while settlement date comes from core-dates: once the BOM moves, confirms would say T+1 date with 63=3. Must change before the BOM pin moves | - |
| confirmation-service | `src/main/resources/application.yml` | mapping, wording | 7, 9 | @ardencm/post-trade-confirms @ardencm/market-ops | 2 | Client narrative/PDF footer say T+2 / two business days | - |
| confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java` | test | 12, 15, 17, 20 | @ardencm/post-trade-confirms | 2 | Test pins T+2 for a 2027-10-11 trade | - |
| core-dates | `CHANGELOG.md` | doc | 13 | @ardencm/post-trade-core | 1 | 1.4.2 entry | - |
| core-dates | `docs/t1-calendar-mismatch.md` | doc | 3 | @ardencm/post-trade-core | 3 | Open question doc; no change | @ardencm/market-ops + Head of Post-Trade |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/FixDates.java` | hard-coded lag, mapping | 26, 34, 35 | @ardencm/post-trade-core | 1 | No change on the 1.4 line: tag 63 mapping already has T_PLUS_1 -> 2; tag 64/75 must stay yyyyMMdd (1.5.0 on main broke this) | - |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java` | hard-coded lag, mapping | 6, 7, 13, 27, 28, 39 | @ardencm/post-trade-core | 1 | Add forMarket(mic, tradeDate) / standard(tradeDate) gated on 2027-10-11 for XLON/XPAR/XETR/XSWX; keep standard()/forMarket(mic) returning T+2 and deprecate | - |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java` | hard-coded lag | 11, 43 | @ardencm/post-trade-core | 1 | CLI default cycle from forMarket(mic, tradeDate) | - |
| core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java` | hard-coded lag | 12 | @ardencm/post-trade-core | 1 | settlementDate(tradeDate, mic) resolves the cycle from the trade date; venue calendar kept (calendar authority escalated) | - |
| core-dates | `src/main/resources/holidays.yaml` | mapping | 6 | @ardencm/post-trade-core @ardencm/market-ops | 3 | No change. Venue vs TARGET2/cash calendar divergence becomes the settlement date under T+1; which calendar is authoritative is a business decision | @ardencm/market-ops + Head of Post-Trade |
| core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java` | test | 11, 13, 18, 38, 42, 43 | @ardencm/post-trade-core | 1 | Post-go-live pins move to T+1; pre-go-live T+2 cases added; tag 64 yyyyMMdd assertion kept | - |
| corporate-actions-service | `docs/ex-date-policy.md` | doc | 7 | @ardencm/asset-servicing | 3 | Policy doc with the open question | Asset-servicing ops, CA-2291 |
| corporate-actions-service | `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java` | hard-coded lag, mapping | 12, 15, 21 | @ardencm/asset-servicing | 3 | EX_DATE_OFFSET_BUSINESS_DAYS = 1 (T+2 regime). Regime for events announced before / record after go-live and dual listings is open (CA-2291) | Asset-servicing ops, CA-2291 |
| corporate-actions-service | `src/main/resources/application.yml` | ex-date | 7, 8 | @ardencm/asset-servicing @ardencm/market-ops | 3 | Dual-listing calendar policy = primary; dual-listed XSWX/XLON ex-date unresolved | Asset-servicing ops, CA-2291 |
| corporate-actions-service | `src/test/java/com/ardencm/assetservicing/corpactions/ExDateCalculatorTest.java` | test | 13 | @ardencm/asset-servicing | 3 | Tests pin T+2 ex-date | Asset-servicing ops, CA-2291 |
| custody-gateway | `contracts/settlement-instruction-service.json` | test pin | 10 | @ardencm/custody-integration | 2 | Contract examples are T+2; add T+1 example with a version bump (owner: custody-integration). Tag 64 pattern unchanged | - |
| custody-gateway | `src/main/resources/application.yml` | mapping, schedule/cut-off | 7, 9 | @ardencm/custody-integration @ardencm/market-ops | 2 | Matching cut-off comment/offset keyed to T+2 (T+1 12:00); under T+1 the cut-off is T or S-0 | - |
| custody-gateway | `src/test/java/com/ardencm/custody/gateway/FixSettlDateParserTest.java` | test | 13, 18 | @ardencm/custody-integration | none | False positive for the cycle: tests tag 64 parsing format (yyyyMMdd), which stays the contract | - |
| fx-funding-service | `README.md` | doc | 10 | @ardencm/treasury-tech | 3 | Documents spot T+2 convention | Treasury (FX funding), via @ardencm/treasury-tech |
| fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java` | FX spot, mapping, market-default caller | 12, 13, 16, 18 | @ardencm/treasury-tech | 3 | FX_SPOT_DAYS = 2 is FX market convention (not CSDR); under T+1 every EUR/GBP/CHF funding becomes tom-next or pre-funded. Funding approach is a treasury decision | Treasury (FX funding), via @ardencm/treasury-tech |
| fx-funding-service | `src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java` | test | 15, 17, 18 | @ardencm/treasury-tech | 3 | Test pins T+2 securities settlement | Treasury (FX funding), via @ardencm/treasury-tech |
| iso20022-fix-messages | `mapping/settlement-cycle.yaml` | mapping | 2, 3, 4, 9, 10, 11, 12 | @ardencm/messaging-standards @ardencm/settlements-core | 1 | EU/UK/CH entries become T+1 / 63=2 with effective_from 2027-10-11 and previous_* values; pre-migration file archived | - |
| iso20022-fix-messages | `samples/fix/AE-xlon-regular-way.fix` | mapping | 1 | @ardencm/messaging-standards | 1 | Golden sample (trade date 2027-10-11) becomes T+1: 63=2, 64=20271012; old sample archived; pre-go-live sample added | - |
| iso20022-fix-messages | `samples/iso20022/sese.023-xlon.xml` | test pin | 8 | @ardencm/messaging-standards | 1 | SttlmDt 2027-10-13 -> 2027-10-12; old sample archived | - |
| iso20022-fix-messages | `tests/test_validate.py` | test | 15 | @ardencm/messaging-standards | 1 | Validate every sample, incl. archive against the archived mapping | - |
| legacy-position-keeper | `src/com/arden/poskeeper/SettleDateUtil.java` | hard-coded lag | 20, 45 | @ardencm/settlements-legacy | 2 | SETTLE_DAYS = 2, JDK 8, cannot take core-dates (PK-118); needs a date-gated local change | - |
| legacy-stored-procs | `README.md` | doc | 5 | @ardencm/settlements-legacy-db | 2 | Docs describe the three lag-carrying objects | - |
| legacy-stored-procs | `procs/usp_CalcSettlementDate.sql` | SQL | 8 | @ardencm/settlements-legacy-db | 2 | @Lag default 2 (London book); Settlements sign-off; DBA change ticket | - |
| legacy-stored-procs | `procs/usp_FlagLateSettlements.sql` | SQL | 4, 8 | @ardencm/settlements-legacy-db | 2 | Hard-coded DATEADD(day, 2) feeds CSDR penalty report; calendar days, not business days | - |
| legacy-stored-procs | `tests/smoke.sql` | test | 3 | @ardencm/settlements-legacy-db | 2 | Smoke expects 2027-10-13 | - |
| legacy-stored-procs | `views/vw_SettlementCalendar.sql` | SQL | 4 | @ardencm/settlements-legacy-db | 2 | NaiveSettlementDate DATEADD(day, 2) for the legacy client statement | - |
| platform-terraform | `batch/schedules.tf` | mapping | 4 | @ardencm/platform-eng | 2 | EventBridge settle-instr-gen cron 06:00 mirrors Autosys (T+2) | - |
| recon-job | `recon/expected.py` | hard-coded lag, mapping | 6, 8, 15 | @ardencm/ops-recon | 2 | DEFAULT_LAG = 2 for every non-NA venue; ignores holidays; would flag every T+1 settlement as early/BREAK | - |
| recon-job | `tests/test_expected.py` | test | 7, 11, 12 | @ardencm/ops-recon | 2 | Test pins T+2 | - |
| risk-lib | `README.md` | doc | 9 | @ardencm/risk-quant | 2 | Documents SETTLEMENT_LAG_DAYS (RISK-2210: move to core-dates calendars) | - |
| risk-lib | `risk_lib/settlement_exposure.py` | hard-coded lag | 8, 11, 12, 13, 14, 30 | @ardencm/risk-quant @ardencm/settlements-core | 2 | SETTLEMENT_LAG_DAYS EU/UK/CH = 2 (regions; no date gate). JP stays 2 | - |
| risk-lib | `tests/test_settlement_exposure.py` | test | 7, 17 | @ardencm/risk-quant | 2 | Tests pin EU T+2 and days_at_risk == 2 | - |
| settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java` | hard-coded lag | 19 | @ardencm/settlements-core | 1 | Resolve cycle with the trade date | - |
| settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java` | hard-coded lag | 11, 28 | @ardencm/settlements-core | 1 | Add cycleFor(mic, tradeDate); explicit pins still win; deprecate cycleFor(mic) | - |
| settlement-instruction-service | `src/main/resources/application.yml` | config, mapping | 11, 12, 13, 14, 15 | @ardencm/settlements-core @ardencm/market-ops | 1 | Drop explicit XLON/XPAR/XETR/XSWX T_PLUS_2 pins so the date gate applies (market-ops sign-off per OPS-RUNBOOK-14) | - |
| settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java` | test | 31 | @ardencm/settlements-core | none | Consumer contract (custody-gateway): keep green, do not edit. Its examples pin XLON T_PLUS_2 explicitly and check tag 64 = yyyyMMdd | - |
| settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java` | test | 15, 20, 22, 23, 31 | @ardencm/settlements-core | 1 | Existing pinned-T+2 cases kept; add unpinned pre/post go-live cases | - |
| settlement-instruction-service | `src/test/resources/contracts/custody-gateway.json` | test | 11 | @ardencm/settlements-core | none | Consumer-owned contract copy; a T+1 example needs a contract bump by @ardencm/custody-integration (tranche 2 follow-up) | - |
| stock-loan-recall-service | `README.md` | doc | 10 | @ardencm/sec-lending | 3 | Notice period is a legal term | Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending |
| stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java` | mapping, market-default caller, recall notice | 13, 14, 16, 19, 21, 22 | @ardencm/sec-lending | 3 | RECALL_NOTICE_BUSINESS_DAYS = 2 from GMSLA schedules; under T+1 the recall deadline falls before trade date. Legal-agreement change | Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending |
| stock-loan-recall-service | `src/test/java/com/ardencm/seclending/recall/RecallServiceTest.java` | test | 11, 13 | @ardencm/sec-lending | 3 | Test pins T+2 recall deadline | Securities lending desk + Legal (GMSLA), via @ardencm/sec-lending |

## Citations

| # | repo | path:line | kind | snippet | CODEOWNERS | tranche |
|---|---|---|---|---|---|---|
| 1 | allocation-service | `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java:17` | hard-coded lag | `private static final int GIVE_UP_SETTLEMENT_DAYS = 2;` | @ardencm/post-trade-allocations | 2 |
| 2 | allocation-service | `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java:21` | hard-coded lag | `? block.tradeDate().plusDays(GIVE_UP_SETTLEMENT_DAYS)` | @ardencm/post-trade-allocations | 2 |
| 3 | allocation-service | `src/main/resources/application.yml:8` | mapping | `default-cycle: T+2` | @ardencm/post-trade-allocations @ardencm/market-ops | 2 |
| 4 | allocation-service | `src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java:16` | test | `void londonBlockSettlesTwoBusinessDaysLater() {` | @ardencm/post-trade-allocations | 2 |
| 5 | allocation-service | `src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java:21` | test | `assertEquals(LocalDate.of(2027, 10, 13), out.get(0).settlementDate());` | @ardencm/post-trade-allocations | 2 |
| 6 | batch-scheduler-config | `calendars/settlement-cutoffs.yaml:3` | mapping | `XLON: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 |
| 7 | batch-scheduler-config | `calendars/settlement-cutoffs.yaml:4` | mapping | `XPAR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 |
| 8 | batch-scheduler-config | `calendars/settlement-cutoffs.yaml:5` | mapping | `XETR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 |
| 9 | batch-scheduler-config | `calendars/settlement-cutoffs.yaml:6` | mapping | `XSWX: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 |
| 10 | batch-scheduler-config | `jil/settlements.jil:15` | schedule/cut-off | `start_times: "06:00"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 11 | batch-scheduler-config | `jil/settlements.jil:18` | mapping | `description: "Generate settlement instructions for trades dated T-1; instructions must reach CSD before T+1 12:00 CET match cut-off for T+2 settlement"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 12 | batch-scheduler-config | `jil/settlements.jil:25` | schedule/cut-off | `start_times: "09:00"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 13 | batch-scheduler-config | `jil/settlements.jil:27` | mapping | `description: "Chase clients for unaffirmed trades; deadline is 12:00 on T+1 (T+2 cycle)"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 14 | batch-scheduler-config | `jil/settlements.jil:34` | schedule/cut-off | `start_times: "15:30"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 15 | batch-scheduler-config | `jil/settlements.jil:36` | mapping | `description: "Issue recalls for sales dated today so shares return before S (T+2)"` | @ardencm/batch-ops @ardencm/settlements-core | 2 |
| 16 | client-portal | `src/server.js:18` | mapping | `res.end(JSON.stringify({ message: 'Trades settle 2 business days after execution (T+2). US equities settle T+1. Amounts shown in the account currency.' }));` | @ardencm/client-digital | 2 |
| 17 | confirmation-service | `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java:21` | market-default caller | `LocalDate settlement = SettlementDates.settlementDate(tradeDate, mic);` | @ardencm/post-trade-confirms | 2 |
| 18 | confirmation-service | `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java:23` | mapping | `// All non-US confirms are regular-way T+2; US moved in May 2024.` | @ardencm/post-trade-confirms | 2 |
| 19 | confirmation-service | `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java:24` | hard-coded lag | `SettlementCycle cycle = mic.startsWith("XN") ? SettlementCycle.T_PLUS_1 : SettlementCycle.T_PLUS_2;` | @ardencm/post-trade-confirms | 2 |
| 20 | confirmation-service | `src/main/resources/application.yml:7` | mapping | `narrative: "This trade will settle on a T+2 basis. Expected settlement date: {settlement}. Please affirm by 12:00 local time on T+1."` | @ardencm/post-trade-confirms @ardencm/market-ops | 2 |
| 21 | confirmation-service | `src/main/resources/application.yml:9` | wording | `footer: "Regular-way settlement is two business days after trade date unless otherwise stated."` | @ardencm/post-trade-confirms @ardencm/market-ops | 2 |
| 22 | confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java:12` | test | `new ConfirmationService("Settles {settlement} on a T+2 basis.");` | @ardencm/post-trade-confirms | 2 |
| 23 | confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java:15` | test | `void parisTradeConfirmsTPlusTwo() {` | @ardencm/post-trade-confirms | 2 |
| 24 | confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java:17` | test | `assertEquals(LocalDate.of(2027, 10, 13), c.settlementDate());` | @ardencm/post-trade-confirms | 2 |
| 25 | confirmation-service | `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java:20` | test | `assertTrue(c.narrative().contains("2027-10-13"));` | @ardencm/post-trade-confirms | 2 |
| 26 | core-dates | `CHANGELOG.md:13` | doc | `- 'SettlementCycle.standard()' remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).` | @ardencm/post-trade-core | 1 |
| 27 | core-dates | `docs/t1-calendar-mismatch.md:3` | doc | `Under T+2, a trading-venue closure that is not a TARGET2 closure (or vice versa) rarely changed` | @ardencm/post-trade-core | 3 |
| 28 | core-dates | `src/main/java/com/ardencm/posttrade/dates/FixDates.java:26` | mapping | `* '3' = T+2, '4' = T+3.` | @ardencm/post-trade-core | 1 |
| 29 | core-dates | `src/main/java/com/ardencm/posttrade/dates/FixDates.java:34` | hard-coded lag | `case T_PLUS_2:` | @ardencm/post-trade-core | 1 |
| 30 | core-dates | `src/main/java/com/ardencm/posttrade/dates/FixDates.java:35` | mapping | `return "3";` | @ardencm/post-trade-core | 1 |
| 31 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:6` | mapping | `* <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland remain on T+2 and are` | @ardencm/post-trade-core | 1 |
| 32 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:7` | hard-coded lag | `* scheduled to move on 11 October 2027. Until then {@link #standard()} returns {@link #T_PLUS_2}` | @ardencm/post-trade-core | 1 |
| 33 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:13` | hard-coded lag | `T_PLUS_2(2),` | @ardencm/post-trade-core | 1 |
| 34 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:27` | hard-coded lag | `public static SettlementCycle standard() {` | @ardencm/post-trade-core | 1 |
| 35 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:28` | hard-coded lag | `return T_PLUS_2;` | @ardencm/post-trade-core | 1 |
| 36 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:39` | hard-coded lag | `return standard();` | @ardencm/post-trade-core | 1 |
| 37 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java:11` | hard-coded lag | `* java -jar core-dates.jar [--cycle T_PLUS_1\|T_PLUS_2] &lt; trades.csv` | @ardencm/post-trade-core | 1 |
| 38 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java:43` | hard-coded lag | `SettlementCycle cycle = override != null ? override : SettlementCycle.forMarket(mic);` | @ardencm/post-trade-core | 1 |
| 39 | core-dates | `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java:12` | hard-coded lag | `return settlementDate(tradeDate, mic, SettlementCycle.forMarket(mic));` | @ardencm/post-trade-core | 1 |
| 40 | core-dates | `src/main/resources/holidays.yaml:6` | mapping | `# but XLON-open). Under T+2 these mismatches were absorbed by the extra day; under` | @ardencm/post-trade-core @ardencm/market-ops | 3 |
| 41 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:11` | test | `void londonTradeSettlesTwoBusinessDaysLater() {` | @ardencm/post-trade-core | 1 |
| 42 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:13` | test | `assertEquals(LocalDate.of(2027, 10, 13), sd);` | @ardencm/post-trade-core | 1 |
| 43 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:18` | test | `// Thursday trade -> Monday settlement under T+2` | @ardencm/post-trade-core | 1 |
| 44 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:38` | test | `assertEquals("2027-10-13", FixDates.tag64(LocalDate.of(2027, 10, 13)));` | @ardencm/post-trade-core | 1 |
| 45 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:42` | test | `void fixTag63ForStandardCycleIsTPlusTwo() {` | @ardencm/post-trade-core | 1 |
| 46 | core-dates | `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:43` | test | `assertEquals("3", FixDates.tag63(SettlementCycle.standard()));` | @ardencm/post-trade-core | 1 |
| 47 | corporate-actions-service | `docs/ex-date-policy.md:7` | doc | `\| T+2 (EU/UK/CH today) \| record date − 1 business day \|` | @ardencm/asset-servicing | 3 |
| 48 | corporate-actions-service | `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:12` | mapping | `* Ex-date = record date minus (settlement cycle - 1) business days. Under T+2 that is one business day` | @ardencm/asset-servicing | 3 |
| 49 | corporate-actions-service | `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:15` | hard-coded lag | `private static final int EX_DATE_OFFSET_BUSINESS_DAYS = 1;` | @ardencm/asset-servicing | 3 |
| 50 | corporate-actions-service | `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:21` | hard-coded lag | `LocalDate exDate = cal.addBusinessDays(recordDate, -EX_DATE_OFFSET_BUSINESS_DAYS);` | @ardencm/asset-servicing | 3 |
| 51 | corporate-actions-service | `src/main/resources/application.yml:7` | ex-date | `# Dual-listed names (XSWX primary, XLON secondary) use the primary listing's calendar for ex-date.` | @ardencm/asset-servicing @ardencm/market-ops | 3 |
| 52 | corporate-actions-service | `src/main/resources/application.yml:8` | ex-date | `dual-listing-calendar-policy: primary` | @ardencm/asset-servicing @ardencm/market-ops | 3 |
| 53 | corporate-actions-service | `src/test/java/com/ardencm/assetservicing/corpactions/ExDateCalculatorTest.java:13` | test | `void exDateIsOneBusinessDayBeforeRecordUnderTPlusTwo() {` | @ardencm/asset-servicing | 3 |
| 54 | custody-gateway | `contracts/settlement-instruction-service.json:10` | test pin | `{ "tradeId": "T-1", "isin": "GB0002634946", "mic": "XLON", "tradeDate": "2027-10-11", "expectedTag64": "20271013" },` | @ardencm/custody-integration | 2 |
| 55 | custody-gateway | `src/main/resources/application.yml:7` | mapping | `# Instructions must reach the CSD before the T+1 12:00 CET matching cut-off for T+2 settlement.` | @ardencm/custody-integration @ardencm/market-ops | 2 |
| 56 | custody-gateway | `src/main/resources/application.yml:9` | schedule/cut-off | `cutoff-offset-days: 1` | @ardencm/custody-integration @ardencm/market-ops | 2 |
| 57 | custody-gateway | `src/test/java/com/ardencm/custody/gateway/FixSettlDateParserTest.java:13` | test | `assertEquals(LocalDate.of(2027, 10, 13), FixSettlDateParser.parse("20271013"));` | @ardencm/custody-integration | none |
| 58 | custody-gateway | `src/test/java/com/ardencm/custody/gateway/FixSettlDateParserTest.java:18` | test | `assertThrows(IllegalArgumentException.class, () -> FixSettlDateParser.parse("2027-10-13"));` | @ardencm/custody-integration | none |
| 59 | fx-funding-service | `README.md:10` | doc | `FX spot value date follows FX market convention (T+2), which is independent of securities settlement cycle. When securities settle before spot value, treasury d` | @ardencm/treasury-tech | 3 |
| 60 | fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:12` | mapping | `/** FX spot convention: value date is T+2 for most pairs. This is an FX market convention, not CSDR. */` | @ardencm/treasury-tech | 3 |
| 61 | fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:13` | FX spot | `private static final int FX_SPOT_DAYS = 2;` | @ardencm/treasury-tech | 3 |
| 62 | fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:16` | market-default caller | `LocalDate secSettle = SettlementDates.settlementDate(tradeDate, mic);` | @ardencm/treasury-tech | 3 |
| 63 | fx-funding-service | `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:18` | FX spot | `LocalDate spotValue = fxCal.addBusinessDays(tradeDate, FX_SPOT_DAYS);` | @ardencm/treasury-tech | 3 |
| 64 | fx-funding-service | `src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java:15` | test | `void parisSecuritiesAndFxSpotAlignUnderTPlusTwo() {` | @ardencm/treasury-tech | 3 |
| 65 | fx-funding-service | `src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java:17` | test | `assertEquals(LocalDate.of(2027, 10, 13), p.securitiesSettlementDate());` | @ardencm/treasury-tech | 3 |
| 66 | fx-funding-service | `src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java:18` | test | `assertEquals(LocalDate.of(2027, 10, 13), p.fxSpotValueDate());` | @ardencm/treasury-tech | 3 |
| 67 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:2` | mapping | `# FIX tag 63 (SettlType): 0=Regular, 1=Cash(T+0), 2=NextDay(T+1), 3=T+2, 4=T+3` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 68 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:3` | mapping | `default_cycle: T+2` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 69 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:4` | mapping | `default_fix_settl_type: "3"` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 70 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:9` | mapping | `XLON: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 71 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:10` | mapping | `XPAR: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 72 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:11` | mapping | `XETR: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 73 | iso20022-fix-messages | `mapping/settlement-cycle.yaml:12` | mapping | `XSWX: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 |
| 74 | iso20022-fix-messages | `samples/fix/AE-xlon-regular-way.fix:1` | mapping | `8=FIX.4.4\|35=AE\|571=T-1\|48=GB0002634946\|22=4\|75=20271011\|63=3\|64=20271013\|207=XLON\|` | @ardencm/messaging-standards | 1 |
| 75 | iso20022-fix-messages | `samples/iso20022/sese.023-xlon.xml:8` | test pin | `<SttlmDt><Dt><Dt>2027-10-13</Dt></Dt></SttlmDt>` | @ardencm/messaging-standards | 1 |
| 76 | iso20022-fix-messages | `tests/test_validate.py:15` | test | `assert validate_fix("8=FIX.4.4\|63=3\|64=2027-10-13\|207=XLON\|", mapping)` | @ardencm/messaging-standards | 1 |
| 77 | legacy-position-keeper | `src/com/arden/poskeeper/SettleDateUtil.java:20` | hard-coded lag | `public static final int SETTLE_DAYS = 2;` | @ardencm/settlements-legacy | 2 |
| 78 | legacy-position-keeper | `src/com/arden/poskeeper/SettleDateUtil.java:45` | hard-coded lag | `while (added < SETTLE_DAYS) {` | @ardencm/settlements-legacy | 2 |
| 79 | legacy-stored-procs | `README.md:5` | doc | `'usp_FlagLateSettlements' (hard-coded 'DATEADD(day, 2, …)') and 'vw_SettlementCalendar' (hard-coded, statement only).` | @ardencm/settlements-legacy-db | 2 |
| 80 | legacy-stored-procs | `procs/usp_CalcSettlementDate.sql:8` | SQL | `@Lag INT = 2,` | @ardencm/settlements-legacy-db | 2 |
| 81 | legacy-stored-procs | `procs/usp_FlagLateSettlements.sql:4` | SQL | `-- A trade is late if not settled by close of T+2 (regular way). Fed to the CSDR penalty report.` | @ardencm/settlements-legacy-db | 2 |
| 82 | legacy-stored-procs | `procs/usp_FlagLateSettlements.sql:8` | SQL | `AND DATEADD(day, 2, t.TradeDate) < CAST(GETDATE() AS DATE);` | @ardencm/settlements-legacy-db | 2 |
| 83 | legacy-stored-procs | `tests/smoke.sql:3` | test | `SELECT @out AS SettlementDate;  -- expect 2027-10-13` | @ardencm/settlements-legacy-db | 2 |
| 84 | legacy-stored-procs | `views/vw_SettlementCalendar.sql:4` | SQL | `DATEADD(day, 2, t.TradeDate) AS NaiveSettlementDate,   -- used by the legacy client statement only` | @ardencm/settlements-legacy-db | 2 |
| 85 | platform-terraform | `batch/schedules.tf:4` | mapping | `schedule_expression = "cron(0 6 ? * MON-FRI *)" # 06:00 London; instructions for T-1 trades, T+2 cycle` | @ardencm/platform-eng | 2 |
| 86 | recon-job | `recon/expected.py:6` | mapping | `# Venue -> business-day lag. Anything missing is assumed regular-way T+2.` | @ardencm/ops-recon | 2 |
| 87 | recon-job | `recon/expected.py:8` | hard-coded lag | `DEFAULT_LAG = 2` | @ardencm/ops-recon | 2 |
| 88 | recon-job | `recon/expected.py:15` | hard-coded lag | `lag = VENUE_LAG.get(mic, DEFAULT_LAG)` | @ardencm/ops-recon | 2 |
| 89 | recon-job | `tests/test_expected.py:7` | test | `assert expected_settlement(date(2027, 10, 11), "XLON") == date(2027, 10, 13)` | @ardencm/ops-recon | 2 |
| 90 | recon-job | `tests/test_expected.py:11` | test | `assert classify(date(2027, 10, 13), date(2027, 10, 14)) == "LATE_MATCH"` | @ardencm/ops-recon | 2 |
| 91 | recon-job | `tests/test_expected.py:12` | test | `assert classify(date(2027, 10, 13), date(2027, 10, 15)) == "BREAK"` | @ardencm/ops-recon | 2 |
| 92 | risk-lib | `README.md:9` | doc | `'SETTLEMENT_LAG_DAYS' drives the days-at-risk horizon. Regions, not venues: the library does not know about` | @ardencm/risk-quant | 2 |
| 93 | risk-lib | `risk_lib/settlement_exposure.py:8` | hard-coded lag | `SETTLEMENT_LAG_DAYS = {` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 94 | risk-lib | `risk_lib/settlement_exposure.py:11` | hard-coded lag | `"EU": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 95 | risk-lib | `risk_lib/settlement_exposure.py:12` | hard-coded lag | `"UK": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 96 | risk-lib | `risk_lib/settlement_exposure.py:13` | hard-coded lag | `"CH": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 97 | risk-lib | `risk_lib/settlement_exposure.py:14` | hard-coded lag | `"JP": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 98 | risk-lib | `risk_lib/settlement_exposure.py:30` | hard-coded lag | `lag = SETTLEMENT_LAG_DAYS[region]` | @ardencm/risk-quant @ardencm/settlements-core | 2 |
| 99 | risk-lib | `tests/test_settlement_exposure.py:7` | test | `assert settlement_date(date(2027, 10, 11), "EU") == date(2027, 10, 13)` | @ardencm/risk-quant | 2 |
| 100 | risk-lib | `tests/test_settlement_exposure.py:17` | test | `assert eu.days_at_risk == 2 and us.days_at_risk == 1` | @ardencm/risk-quant | 2 |
| 101 | settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java:19` | hard-coded lag | `SettlementCycle cycle = config.cycleFor(mic);` | @ardencm/settlements-core | 1 |
| 102 | settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java:11` | hard-coded lag | `* Per-venue settlement cycle overrides. Anything not listed falls back to {@link SettlementCycle#standard()}.` | @ardencm/settlements-core | 1 |
| 103 | settlement-instruction-service | `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java:28` | hard-coded lag | `return cycles.getOrDefault(mic, SettlementCycle.standard());` | @ardencm/settlements-core | 1 |
| 104 | settlement-instruction-service | `src/main/resources/application.yml:11` | mapping | `# EU/UK/CH venues: regular way T+2 (CSDR art. 5). Migration to T+1 scheduled 11 Oct 2027 — see SETTLE-4471.` | @ardencm/settlements-core @ardencm/market-ops | 1 |
| 105 | settlement-instruction-service | `src/main/resources/application.yml:12` | config | `XLON: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 |
| 106 | settlement-instruction-service | `src/main/resources/application.yml:13` | config | `XPAR: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 |
| 107 | settlement-instruction-service | `src/main/resources/application.yml:14` | config | `XETR: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 |
| 108 | settlement-instruction-service | `src/main/resources/application.yml:15` | config | `XSWX: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 |
| 109 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java:31` | test | `cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));` | @ardencm/settlements-core | none |
| 110 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:15` | test | `cfg.setCycles(Map.of("XNYS", SettlementCycle.T_PLUS_1, "XLON", SettlementCycle.T_PLUS_2));` | @ardencm/settlements-core | 1 |
| 111 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:20` | test | `void londonInstructionSettlesTPlusTwo() {` | @ardencm/settlements-core | 1 |
| 112 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:22` | test | `assertEquals(LocalDate.of(2027, 10, 13), i.settlementDate());` | @ardencm/settlements-core | 1 |
| 113 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:23` | test | `assertEquals("20271013", i.fixSettlDate());` | @ardencm/settlements-core | 1 |
| 114 | settlement-instruction-service | `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:31` | test | `assertTrue(fix.contains("\u000164=20271013\u0001"), fix);` | @ardencm/settlements-core | 1 |
| 115 | settlement-instruction-service | `src/test/resources/contracts/custody-gateway.json:11` | test | `{ "tradeId": "T-1", "isin": "GB0002634946", "mic": "XLON", "tradeDate": "2027-10-11", "expectedTag64": "20271013" },` | @ardencm/settlements-core | none |
| 116 | stock-loan-recall-service | `README.md:10` | doc | `Recall notice period comes from GMSLA schedules (2 business days). Any change is a legal-agreement change, not just a config change.` | @ardencm/sec-lending | 3 |
| 117 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:13` | recall notice | `* Standard recall notice period agreed in our GMSLA schedules: borrower returns within 2 business days.` | @ardencm/sec-lending | 3 |
| 118 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:14` | mapping | `* Under T+2 a recall issued on trade date lands on settlement date; there is no slack.` | @ardencm/sec-lending | 3 |
| 119 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:16` | recall notice | `static final int RECALL_NOTICE_BUSINESS_DAYS = 2;` | @ardencm/sec-lending | 3 |
| 120 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:19` | market-default caller | `LocalDate settle = SettlementDates.settlementDate(saleTradeDate, mic);` | @ardencm/sec-lending | 3 |
| 121 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:21` | recall notice | `LocalDate deadline = cal.addBusinessDays(settle, -RECALL_NOTICE_BUSINESS_DAYS);` | @ardencm/sec-lending | 3 |
| 122 | stock-loan-recall-service | `src/main/java/com/ardencm/seclending/recall/RecallService.java:22` | recall notice | `return new RecallNotice(loanId, isin, saleTradeDate, settle, deadline, RECALL_NOTICE_BUSINESS_DAYS);` | @ardencm/sec-lending | 3 |
| 123 | stock-loan-recall-service | `src/test/java/com/ardencm/seclending/recall/RecallServiceTest.java:11` | test | `void recallDeadlineIsTradeDateUnderTPlusTwo() {` | @ardencm/sec-lending | 3 |
| 124 | stock-loan-recall-service | `src/test/java/com/ardencm/seclending/recall/RecallServiceTest.java:13` | test | `assertEquals(LocalDate.of(2027, 10, 13), n.saleSettlementDate());` | @ardencm/sec-lending | 3 |
