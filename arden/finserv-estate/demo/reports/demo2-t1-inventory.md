# SETTLE-4471 — settlement-cycle inventory (Arden estate)

Programme T1-2027 · scope **EU, UK and CH regular-way trades** · go-live **2027-10-11** · generated 2026-10-23T03:07:11Z from `~/finserv-workspace` (20 repos).

## Headline

- **57 files** with **105 line citations** across **17 of 20 repositories**.
- By file: tranche 1 (instruction path) **18**, tranche 2 (downstream / schedules / wording / SQL) **21**, tranche 3 (business decision first) **16**, no change (wire contract kept) **2**.
- Decisions owed (tranche 3): @ardencm/market-ops + Head of Post-Trade; Asset-servicing ops (CA-2291); Head of Post-Trade (London position roll system owner, PK-118); Securities-lending desk head + Legal (GMSLA schedules); Treasury funding desk (Head of Treasury).
- Every citation below has repo/path/line, a snippet, CODEOWNERS owners and a tranche/action. Keyword hits that are not cycle assumptions are listed at the end with the reason.

Search terms: `T_PLUS_2`, `T+2`, `T+1`, `plusDays(`, `DATEADD`, `SettlTyp`/tag 63, `default_cycle`/`default-cycle`, `*_DAYS = 2`, `@Lag`, scheduler `start_times`/`cron`, FX spot, recall notice, ex-date; every hit read in context.

## allocation-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/resources/application.yml:8` | config | allocation confirms default T+2 | `default-cycle: T+2` | @ardencm/post-trade-allocations @ardencm/market-ops | 2 | change: date-gate / derive from core-dates — **@ardencm/market-ops** |
| `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java:17` | hard-coded lag | give-ups = trade date + 2 calendar days (ignores weekends/holidays) | `private static final int GIVE_UP_SETTLEMENT_DAYS = 2;` | @ardencm/post-trade-allocations | 2 | change after ops confirms give-up rule — **Post-trade ops (2019 give-up instruction)** |
| `src/main/java/com/ardencm/posttrade/allocation/AllocationService.java:21` | hard-coded lag | plusDays(2) calendar arithmetic | `? block.tradeDate().plusDays(GIVE_UP_SETTLEMENT_DAYS)` | @ardencm/post-trade-allocations | 2 | change after ops confirms — **Post-trade ops (2019 give-up instruction)** |
| `src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java:16` | test | post-go-live XLON block expected T+2 (passes until BOM moves) | `void londonBlockSettlesTwoBusinessDaysLater() {` | @ardencm/post-trade-allocations | 2 | change with BOM move |
| `src/test/java/com/ardencm/posttrade/allocation/AllocationServiceTest.java:25` | test | give-up = +2 calendar days | `void giveUpUsesCalendarDays() {` | @ardencm/post-trade-allocations | 2 | change after ops confirms — **Post-trade ops (2019 give-up instruction)** |

## batch-scheduler-config

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `jil/settlements.jil:15` | schedule | SETTLE_INSTR_GEN 06:00 next morning for T-1 trades | `start_times: "06:00"` | @ardencm/batch-ops @ardencm/settlements-core | 2 | change: T+1 needs same-day/T-evening run — **@ardencm/market-ops** |
| `jil/settlements.jil:18` | schedule | instruction run keyed to T+1 morning | `description: "Generate settlement instructions for trades dated T-1; instructions must reach CSD before T+1 12:00 CET match cut-off for T+2 settlement"` | @ardencm/batch-ops @ardencm/settlements-core | 2 | change |
| `jil/settlements.jil:27` | schedule | AFFIRMATION_CHASER 09:00 T+1 | `description: "Chase clients for unaffirmed trades; deadline is 12:00 on T+1 (T+2 cycle)"` | @ardencm/batch-ops @ardencm/settlements-core | 2 | change: affirmation on T |
| `jil/settlements.jil:36` | schedule | RECALL_SWEEP 15:30 assumes S = T+2 | `description: "Issue recalls for sales dated today so shares return before S (T+2)"` | @ardencm/batch-ops @ardencm/settlements-core | 2 | change after GMSLA decision (tranche 3) — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `calendars/settlement-cutoffs.yaml:3` | config | XLON cut-offs on T+1, cycle T+2 | `XLON: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 | change — **@ardencm/market-ops** |
| `calendars/settlement-cutoffs.yaml:4` | config | XPAR cut-offs on T+1, cycle T+2 | `XPAR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 | change — **@ardencm/market-ops** |
| `calendars/settlement-cutoffs.yaml:5` | config | XETR cut-offs on T+1, cycle T+2 | `XETR: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 | change — **@ardencm/market-ops** |
| `calendars/settlement-cutoffs.yaml:6` | config | XSWX cut-offs on T+1, cycle T+2 | `XSWX: { affirmation: "12:00 T+1", csd_matching: "12:00 T+1", cycle: T+2 }` | @ardencm/batch-ops | 2 | change — **@ardencm/market-ops** |

## client-portal

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/server.js:18` | doc | client-facing wording says T+2 | `res.end(JSON.stringify({ message: 'Trades settle 2 business days after execution (T+2). US equities settle T+1. Amounts shown in the account currency.' }));` | @ardencm/client-digital | 2 | change: cycle-aware wording (not in tranche 1) |

## common-java-bom

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `pom.xml:18` | config | consumers pinned to core-dates 1.4.0 (T+2 behaviour) | `<core-dates.version>1.4.0</core-dates.version>` | @ardencm/platform-eng | none | no change — BOM pin not moved (needs platform-eng ADR + tranche 2 consumers ready) — **@ardencm/platform-eng** |

## confirmation-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java:23` | hard-coded lag | non-US confirms are T+2 | `// All non-US confirms are regular-way T+2; US moved in May 2024.` | @ardencm/post-trade-confirms | 2 | change: take cycle from core-dates forTrade — must land before BOM move (else tag 63 '3' with T+1 date) |
| `src/main/java/com/ardencm/posttrade/confirms/ConfirmationService.java:24` | hard-coded lag | tag 63 hard-coded '3' for non-XN MICs | `SettlementCycle cycle = mic.startsWith("XN") ? SettlementCycle.T_PLUS_1 : SettlementCycle.T_PLUS_2;` | @ardencm/post-trade-confirms | 2 | change (as above) |
| `src/main/resources/application.yml:7` | doc | client confirm narrative says T+2, affirm by T+1 | `narrative: "This trade will settle on a T+2 basis. Expected settlement date: {settlement}. Please affirm by 12:00 local time on T+1."` | @ardencm/post-trade-confirms @ardencm/market-ops | 2 | change: cycle-aware wording — **@ardencm/market-ops** |
| `src/main/resources/application.yml:9` | doc | PDF footer says two business days | `footer: "Regular-way settlement is two business days after trade date unless otherwise stated."` | @ardencm/post-trade-confirms @ardencm/market-ops | 2 | change: wording |
| `src/test/java/com/ardencm/posttrade/confirms/ConfirmationServiceTest.java:15` | test | post-go-live XPAR confirm expected T+2 / tag 63 '3' | `void parisTradeConfirmsTPlusTwo() {` | @ardencm/post-trade-confirms | 2 | change with service |

## core-dates

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:7` | doc | Javadoc: EU/UK/CH stay T+2 until 11 Oct 2027; standard() returns T+2 | `* scheduled to move on 11 October 2027. Until then {@link #standard()} returns {@link #T_PLUS_2}` | @ardencm/post-trade-core | 1 | change: document trade-date gate |
| `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:28` | hard-coded lag | standard() = T+2 regardless of trade date | `return T_PLUS_2;` | @ardencm/post-trade-core | 1 | change: keep (pre-go-live), deprecate; add forTrade(mic, tradeDate) |
| `src/main/java/com/ardencm/posttrade/dates/SettlementCycle.java:39` | hard-coded lag | forMarket(mic) falls back to T+2 for every non-NA MIC | `return standard();` | @ardencm/post-trade-core | 1 | change: keep legacy, deprecate; date-gated variant |
| `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java:12` | hard-coded lag | 2-arg settlementDate() uses undated market default (T+2) | `return settlementDate(tradeDate, mic, SettlementCycle.forMarket(mic));` | @ardencm/post-trade-core | 1 | change: use forTrade(mic, tradeDate) |
| `src/main/java/com/ardencm/posttrade/dates/SettlementDates.java:26` | hard-coded lag | affirmation deadline = S-1 (under T+1 collapses onto trade date) | `return BusinessCalendar.of(mic).addBusinessDays(settlementDate, -1);` | @ardencm/post-trade-core | 1 | no change in tranche 1 (cut-off convention → tranche 2 batch/cut-offs) — **@ardencm/market-ops** |
| `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java:15` | doc | CLI default = undated market cycle | `* Without {@code --cycle} the market default from {@link SettlementCycle#forMarket(String)} is used.` | @ardencm/post-trade-core | 1 | change: document trade-date gate |
| `src/main/java/com/ardencm/posttrade/dates/SettlementDateCli.java:43` | hard-coded lag | CLI resolves cycle without trade date | `SettlementCycle cycle = override != null ? override : SettlementCycle.forMarket(mic);` | @ardencm/post-trade-core | 1 | change: forTrade(mic, tradeDate) |
| `src/main/java/com/ardencm/posttrade/dates/FixDates.java:10` | mapping | tag 64/75 format (yyyyMMdd on release/1.4; ISO yyyy-MM-dd on main/1.5.0) | `private static final DateTimeFormatter LOCAL_MKT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;` | @ardencm/post-trade-core | 1 | no change — wire contract; release cut from release/1.4 (see deviation) |
| `src/main/java/com/ardencm/posttrade/dates/FixDates.java:34` | mapping | tag 63: T+2 → '3', T+1 → '2' | `case T_PLUS_2:` | @ardencm/post-trade-core | 1 | no change — mapping already correct, follows the cycle |
| `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:11` | test | post-go-live XLON trade (2027-10-11) expected T+2 | `void londonTradeSettlesTwoBusinessDaysLater() {` | @ardencm/post-trade-core | 1 | change: pre-/post-go-live cases |
| `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:18` | test | post-go-live weekend case expects T+2 | `// Thursday trade -> Monday settlement under T+2` | @ardencm/post-trade-core | 1 | change |
| `src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java:43` | test | tag 63 of standard() = '3' | `assertEquals("3", FixDates.tag63(SettlementCycle.standard()));` | @ardencm/post-trade-core | 1 | keep (legacy entry point unchanged) + add dated case |
| `src/main/resources/holidays.yaml:6` | doc | T+2 absorbed venue/TARGET2 calendar mismatches; T+1 surfaces them | `# but XLON-open). Under T+2 these mismatches were absorbed by the extra day; under` | @ardencm/post-trade-core @ardencm/market-ops | 3 | no change — calendar authority is a business decision — **@ardencm/market-ops + Head of Post-Trade** |
| `src/main/resources/holidays.yaml:43` | config | separate cash-leg calendar diverges from venue calendars | `TARGET2: # Eurosystem payment system closures (cash leg)` | @ardencm/post-trade-core @ardencm/market-ops | 3 | no change — calendar authority — **@ardencm/market-ops + Head of Post-Trade** |
| `docs/t1-calendar-mismatch.md:5` | doc | open question: venue vs CSD vs cash calendar under T+1 | `is the very next business day, so **which calendar is authoritative** — the venue's, the CSD's, or` | @ardencm/post-trade-core | 3 | no change — evidence for escalation — **@ardencm/market-ops + Head of Post-Trade** |
| `README.md:6` | doc | README example uses undated market default | `LocalDate sd = SettlementDates.settlementDate(tradeDate, "XLON");   // market default cycle` | @ardencm/post-trade-core | 1 | change: document trade-date gate |
| `CHANGELOG.md:12` | doc | changelog states standard() stays T+2 | `- `SettlementCycle.standard()` remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).` | @ardencm/post-trade-core | 1 | change: add 1.4.2 entry |

## corporate-actions-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:12` | doc | ex-date regime depends on cycle | `* Ex-date = record date minus (settlement cycle - 1) business days. Under T+2 that is one business day` | @ardencm/asset-servicing | 3 | escalate (CA-2291) — **Asset-servicing ops (CA-2291)** |
| `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:15` | hard-coded lag | ex-date = record − 1 BD (T+2 regime) | `private static final int EX_DATE_OFFSET_BUSINESS_DAYS = 1;` | @ardencm/asset-servicing | 3 | no change in code — ex-date regime — **Asset-servicing ops (CA-2291)** |
| `src/main/java/com/ardencm/assetservicing/corpactions/ExDateCalculator.java:19` | config | non-XLON ex-dates use TARGET2 not venue calendar | `String calendar = primaryMic.equals("XLON") ? "XLON" : "TARGET2";` | @ardencm/asset-servicing | 3 | escalate (calendar authority) — **@ardencm/market-ops + Head of Post-Trade** |
| `src/main/resources/application.yml:8` | config | dual-listed XSWX/XLON use primary calendar | `dual-listing-calendar-policy: primary` | @ardencm/asset-servicing @ardencm/market-ops | 3 | escalate (dual listings) — **Asset-servicing ops (CA-2291)** |
| `docs/ex-date-policy.md:7` | doc | T+2 regime ex-date | `\| T+2 (EU/UK/CH today) \| record date − 1 business day \|` | @ardencm/asset-servicing | 3 | evidence — **Asset-servicing ops (CA-2291)** |
| `docs/ex-date-policy.md:10` | doc | straddling events unresolved | `Open question logged in CA-2291: for events **announced before** 11 Oct 2027 with **record dates after** it,` | @ardencm/asset-servicing | 3 | evidence — **Asset-servicing ops (CA-2291)** |
| `src/test/java/com/ardencm/assetservicing/corpactions/ExDateCalculatorTest.java:13` | test | post-go-live record date (2027-10-15) uses T+2 ex-date | `void exDateIsOneBusinessDayBeforeRecordUnderTPlusTwo() {` | @ardencm/asset-servicing | 3 | escalate — **Asset-servicing ops (CA-2291)** |

## custody-gateway

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/resources/application.yml:7` | config | CSD matching cut-off = T+1 12:00 (offset 1 day) | `# Instructions must reach the CSD before the T+1 12:00 CET matching cut-off for T+2 settlement.` | @ardencm/custody-integration @ardencm/market-ops | 2 | change: cut-off offset for T+1 — **@ardencm/market-ops** |
| `src/main/resources/application.yml:9` | config | cut-off one day after trade date | `cutoff-offset-days: 1` | @ardencm/custody-integration @ardencm/market-ops | 2 | change — **@ardencm/market-ops** |
| `contracts/settlement-instruction-service.json:10` | test | contract example: 2027-10-11 XLON → T+2 | `{ "tradeId": "T-1", "isin": "GB0002634946", "mic": "XLON", "tradeDate": "2027-10-11", "expectedTag64": "20271013" },` | @ardencm/custody-integration | 2 | contract bump with T+1 examples (custody-integration owns) |
| `contracts/settlement-instruction-service.json:7` | test | tag 64 YYYYMMDD | `"tag64": { "format": "LocalMktDate", "pattern": "^[0-9]{8}$" }` | @ardencm/custody-integration | none | no change — wire contract kept |
| `src/main/java/com/ardencm/custody/gateway/FixSettlDateParser.java:14` | mapping | custodian parser requires YYYYMMDD | `throw new IllegalArgumentException("tag 64 must be LocalMktDate (YYYYMMDD): " + tag64);` | @ardencm/custody-integration | none | no change — wire contract kept |

## fx-funding-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:12` | doc | FX spot T+2 is market convention, independent of CSDR | `/** FX spot convention: value date is T+2 for most pairs. This is an FX market convention, not CSDR. */` | @ardencm/treasury-tech | 3 | escalate: tom-next vs pre-funding policy for every EUR/GBP/CHF trade after go-live — **Treasury funding desk (Head of Treasury)** |
| `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:13` | hard-coded lag | FX spot lag 2 | `private static final int FX_SPOT_DAYS = 2;` | @ardencm/treasury-tech | 3 | no change in code — FX value dates are a treasury decision — **Treasury funding desk (Head of Treasury)** |
| `src/main/java/com/ardencm/treasury/fxfunding/FxFundingService.java:20` | hard-coded lag | tom-next triggers whenever securities settle before spot (all EU/UK/CH after go-live) | `boolean tomNext = secSettle.isBefore(spotValue);` | @ardencm/treasury-tech | 3 | escalate — **Treasury funding desk (Head of Treasury)** |
| `src/test/java/com/ardencm/treasury/fxfunding/FxFundingServiceTest.java:15` | test | post-go-live XPAR securities and FX spot align (T+2) | `void parisSecuritiesAndFxSpotAlignUnderTPlusTwo() {` | @ardencm/treasury-tech | 3 | escalate (test breaks once BOM moves) — **Treasury funding desk (Head of Treasury)** |
| `README.md:10` | doc | documents FX spot T+2 | `FX spot value date follows FX market convention (T+2), which is independent of securities settlement cycle. When securities settle before spot value, treasury d` | @ardencm/treasury-tech | 3 | escalate — **Treasury funding desk (Head of Treasury)** |

## iso20022-fix-messages

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `mapping/settlement-cycle.yaml:3` | mapping | default cycle T+2 | `default_cycle: T+2` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change: effective_from-versioned mapping |
| `mapping/settlement-cycle.yaml:4` | mapping | default tag 63 = '3' | `default_fix_settl_type: "3"` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change |
| `mapping/settlement-cycle.yaml:9` | mapping | XLON T+2 / tag 63 '3' | `XLON: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change: '2' from 2027-10-11 |
| `mapping/settlement-cycle.yaml:10` | mapping | XPAR T+2 / tag 63 '3' | `XPAR: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change |
| `mapping/settlement-cycle.yaml:11` | mapping | XETR T+2 / tag 63 '3' | `XETR: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change |
| `mapping/settlement-cycle.yaml:12` | mapping | XSWX T+2 / tag 63 '3' | `XSWX: { cycle: T+2, fix_settl_type: "3" }` | @ardencm/messaging-standards @ardencm/settlements-core | 1 | change |
| `validate.py:12` | mapping | parser only understands one undated mapping | `m = re.match(r"\s+(X[A-Z]{3}): \{ cycle: (T\+\d), fix_settl_type: \"(\d)\" \}", line)` | @ardencm/messaging-standards | 1 | change: pick mapping version by tag 75 |
| `validate.py:26` | mapping | expected tag 63 ignores trade date | `expected = mapping["markets"].get(fields.get("207"), mapping["default"])["fix_settl_type"]` | @ardencm/messaging-standards | 1 | change |
| `validate.py:24` | mapping | tag 64 must be YYYYMMDD | `if not re.fullmatch(r"\d{8}", fields.get("64", "")):` | @ardencm/messaging-standards | 1 | no change — wire contract |
| `samples/fix/AE-xlon-regular-way.fix:1` | test | golden FIX sample: 2027-10-11 XLON trade, T+2, tag 63 '3' | `8=FIX.4.4\|35=AE\|571=T-1\|48=GB0002634946\|22=4\|75=20271011\|63=3\|64=20271013\|207=XLON\|` | @ardencm/messaging-standards | 1 | change: archive under samples/archive/, add pre/post go-live samples |
| `samples/iso20022/sese.023-xlon.xml:8` | test | golden sese.023: 2027-10-11 trade settles 2027-10-13 (T+2) | `<SttlmDt><Dt><Dt>2027-10-13</Dt></Dt></SttlmDt>` | @ardencm/messaging-standards | 1 | change: archive + T+1 sample |
| `tests/test_validate.py:15` | test | ISO date in tag 64 rejected | `assert validate_fix("8=FIX.4.4\|63=3\|64=2027-10-13\|207=XLON\|", mapping)` | @ardencm/messaging-standards | 1 | no change — contract guard |

## legacy-position-keeper

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/com/arden/poskeeper/SettleDateUtil.java:20` | hard-coded lag | London position roll hard-coded T+2 (JDK 8, cannot use core-dates — PK-118) | `public static final int SETTLE_DAYS = 2;` | @ardencm/settlements-legacy | 3 | escalate: patch-in-place vs re-platform/decommission — **Head of Post-Trade (London position roll system owner, PK-118)** |
| `src/com/arden/poskeeper/SettleDateUtil.java:12` | doc | cannot consume core-dates (JDK 8) | `* Settlement date utility. NB: this class pre-dates core-dates; do NOT replace with core-dates without` | @ardencm/settlements-legacy | 3 | escalate — **Head of Post-Trade (London position roll system owner, PK-118)** |
| `src/holidays.properties:1` | config | hand-maintained London-only calendar, diverges from core-dates | `# London only. Maintained by hand each November by ops (see PK-73). Format yyyyMMdd.` | @ardencm/settlements-legacy | 3 | escalate (calendar authority) — **@ardencm/market-ops + Head of Post-Trade** |
| `README.txt:5` | doc | local settlement arithmetic | `Settlement dates are computed locally (SettleDateUtil) - this predates core-dates.` | @ardencm/settlements-legacy | 3 | escalate — **Head of Post-Trade (London position roll system owner, PK-118)** |

## legacy-stored-procs

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `procs/usp_CalcSettlementDate.sql:8` | SQL | London book default lag 2 | `@Lag INT = 2,` | @ardencm/settlements-legacy-db | 2 | change via DBA change ticket: date-gated default |
| `procs/usp_FlagLateSettlements.sql:4` | SQL | late = not settled by T+2 (feeds CSDR penalty report) | `-- A trade is late if not settled by close of T+2 (regular way). Fed to the CSDR penalty report.` | @ardencm/settlements-legacy-db | 2 | change via DBA ticket |
| `procs/usp_FlagLateSettlements.sql:8` | SQL | hard-coded DATEADD(day, 2) calendar days | `AND DATEADD(day, 2, t.TradeDate) < CAST(GETDATE() AS DATE);` | @ardencm/settlements-legacy-db | 2 | change via DBA ticket |
| `views/vw_SettlementCalendar.sql:4` | SQL | naive settlement date (legacy client statement) | `DATEADD(day, 2, t.TradeDate) AS NaiveSettlementDate,   -- used by the legacy client statement only` | @ardencm/settlements-legacy-db | 2 | change via DBA ticket |
| `tests/smoke.sql:3` | test | UAT smoke expects T+2 for 2027-10-11 | `SELECT @out AS SettlementDate;  -- expect 2027-10-13` | @ardencm/settlements-legacy-db | 2 | change |
| `README.md:4` | doc | documents the three lag sites | `change ticket, not CI. Three objects carry the settlement lag: `usp_CalcSettlementDate` (parameterised, default 2),` | @ardencm/settlements-legacy-db | 2 | update |

## platform-terraform

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `batch/schedules.tf:4` | schedule | EventBridge 06:00 instruction run for T-1 trades | `schedule_expression = "cron(0 6 ? * MON-FRI *)" # 06:00 London; instructions for T-1 trades, T+2 cycle` | @ardencm/platform-eng | 2 | change in step with Autosys |

## recon-job

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `recon/expected.py:6` | hard-coded lag | non-NA venues assumed T+2 | `# Venue -> business-day lag. Anything missing is assumed regular-way T+2.` | @ardencm/ops-recon | 2 | change: date-gate or read core-dates output |
| `recon/expected.py:8` | hard-coded lag | default lag 2 | `DEFAULT_LAG = 2` | @ardencm/ops-recon | 2 | change |
| `recon/expected.py:11` | config | +1 day late-match tolerance sized for T+2 | `LATE_MATCH_TOLERANCE_DAYS = 1` | @ardencm/ops-recon | 2 | review with ops-recon |
| `tests/test_expected.py:7` | test | post-go-live XLON expected T+2 | `assert expected_settlement(date(2027, 10, 11), "XLON") == date(2027, 10, 13)` | @ardencm/ops-recon | 2 | change |

## risk-lib

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `risk_lib/settlement_exposure.py:11` | hard-coded lag | EU lag 2 | `"EU": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 | change: date-gated region lag |
| `risk_lib/settlement_exposure.py:12` | hard-coded lag | UK lag 2 | `"UK": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 | change |
| `risk_lib/settlement_exposure.py:13` | hard-coded lag | CH lag 2 | `"CH": 2,` | @ardencm/risk-quant @ardencm/settlements-core | 2 | change |
| `tests/test_settlement_exposure.py:6` | test | post-go-live EU expected T+2 | `def test_eu_trade_settles_two_business_days_later():` | @ardencm/risk-quant | 2 | change |
| `tests/test_settlement_exposure.py:17` | test | EU days at risk 2 | `assert eu.days_at_risk == 2 and us.days_at_risk == 1` | @ardencm/risk-quant | 2 | change |

## settlement-instruction-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/resources/application.yml:11` | doc | comment: EU/UK/CH regular way T+2 | `# EU/UK/CH venues: regular way T+2 (CSDR art. 5). Migration to T+1 scheduled 11 Oct 2027 — see SETTLE-4471.` | @ardencm/settlements-core @ardencm/market-ops | 1 | change |
| `src/main/resources/application.yml:12` | config | explicit T+2 pin XLON | `XLON: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 | change: drop pin (date-gated default applies) |
| `src/main/resources/application.yml:13` | config | explicit T+2 pin XPAR | `XPAR: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 | change: drop pin |
| `src/main/resources/application.yml:14` | config | explicit T+2 pin XETR | `XETR: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 | change: drop pin |
| `src/main/resources/application.yml:15` | config | explicit T+2 pin XSWX | `XSWX: T_PLUS_2` | @ardencm/settlements-core @ardencm/market-ops | 1 | change: drop pin |
| `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java:11` | doc | fallback documented as T+2 | `* Per-venue settlement cycle overrides. Anything not listed falls back to {@link SettlementCycle#standard()}.` | @ardencm/settlements-core | 1 | change |
| `src/main/java/com/ardencm/settlements/instruction/SettlementCycleConfig.java:28` | hard-coded lag | fallback ignores trade date | `return cycles.getOrDefault(mic, SettlementCycle.standard());` | @ardencm/settlements-core | 1 | change: cycleFor(mic, tradeDate); legacy cycleFor(mic) deprecated |
| `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java:19` | hard-coded lag | instruction cycle resolved without trade date | `SettlementCycle cycle = config.cycleFor(mic);` | @ardencm/settlements-core | 1 | change |
| `src/main/java/com/ardencm/settlements/instruction/InstructionBuilder.java:25` | doc | tag 64 = YYYYMMDD (wire contract) | `/** FIX 4.4 tag=value rendering. Custodians parse tag 64 as LocalMktDate (YYYYMMDD). */` | @ardencm/settlements-core | 1 | no change — contract |
| `src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java:20` | test | post-go-live XLON instruction expected T+2 / tag 63 '3' | `void londonInstructionSettlesTPlusTwo() {` | @ardencm/settlements-core | 1 | change: pre-/post-go-live cases |
| `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java:26` | test | consumer contract: tag 64 YYYYMMDD | `@DisplayName("contract: tag 64 (SettlDate) is FIX LocalMktDate YYYYMMDD")` | @ardencm/settlements-core | 1 | no change — consumer contract, must stay green |
| `src/test/java/com/ardencm/settlements/instruction/CustodyGatewayContractTest.java:31` | test | contract fixture pins XLON T+2 so 2027-10-11 → 20271013 stays valid | `cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));` | @ardencm/settlements-core | 1 | no change — consumer contract (T+1 examples = contract bump, tranche 2) — **@ardencm/custody-integration** |
| `src/test/resources/contracts/custody-gateway.json:11` | test | contract example: post-go-live XLON trade settles T+2 | `{ "tradeId": "T-1", "isin": "GB0002634946", "mic": "XLON", "tradeDate": "2027-10-11", "expectedTag64": "20271013" },` | @ardencm/settlements-core | 1 | no change — owned by custody-gateway; bump in tranche 2 — **@ardencm/custody-integration** |

## stock-loan-recall-service

| file:line | kind | assumes | snippet | CODEOWNERS | tranche | action / decision owner |
|---|---|---|---|---|---|---|
| `src/main/java/com/ardencm/seclending/recall/RecallService.java:13` | doc | GMSLA recall notice 2 business days | `* Standard recall notice period agreed in our GMSLA schedules: borrower returns within 2 business days.` | @ardencm/sec-lending | 3 | escalate: under T+1 deadline = T-1 (before the sale) — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `src/main/java/com/ardencm/seclending/recall/RecallService.java:16` | hard-coded lag | recall notice period 2 | `static final int RECALL_NOTICE_BUSINESS_DAYS = 2;` | @ardencm/sec-lending | 3 | no change in code — contractual — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `src/main/java/com/ardencm/seclending/recall/RecallService.java:21` | hard-coded lag | deadline = S - 2 | `LocalDate deadline = cal.addBusinessDays(settle, -RECALL_NOTICE_BUSINESS_DAYS);` | @ardencm/sec-lending | 3 | escalate — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `src/main/resources/application.yml:7` | config | recall cut-off 15:00 | `# Recalls issued after 15:00 London count from the next business day.` | @ardencm/sec-lending @ardencm/market-ops | 3 | escalate (with notice period) — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `src/test/java/com/ardencm/seclending/recall/RecallServiceTest.java:11` | test | post-go-live sale: deadline = trade date under T+2 | `void recallDeadlineIsTradeDateUnderTPlusTwo() {` | @ardencm/sec-lending | 3 | escalate — **Securities-lending desk head + Legal (GMSLA schedules)** |
| `README.md:10` | doc | legal-agreement change | `Recall notice period comes from GMSLA schedules (2 business days). Any change is a legal-agreement change, not just a config change.` | @ardencm/sec-lending | 3 | escalate — **Securities-lending desk head + Legal (GMSLA schedules)** |

## Reviewed and excluded (no change)

| repo | files | reason |
|---|---|---|
| stock-loan-recall-service | pom.xml, ci/settings.xml, StockLoanRecallServiceApplication.java, RecallController.java, RecallNotice.java | 'recall' matches artifact/class names, no cycle arithmetic |
| market-data-feed-contracts | README.md, ardenfeed/VENDOR-NOTICE-2026-09-11.md | 'notice' = vendor schema notice |
| platform-terraform | artifactory/ci-tokens.tf:15 | repo names in a token list |
| settlement-instruction-service | Instruction.java:6 | field name fixSettlType only |
| fx-funding-service | pom.xml:18, FundingPlan.java:6 | 'spot' in description/field names; logic cited in FxFundingService.java |
| corporate-actions-service | pom.xml:18, README.md:3, DividendEvent.java:6, CorporateActionsController.java | 'ex-date' in names/route; logic cited in ExDateCalculator.java |
| confirmation-service | Confirmation.java:6 | field name settlTypeTag63 |
| iso20022-fix-messages | README.md:4 | mentions the mapping file only |
| eod-pricing-batch, ci-shared-workflows, market-data-feed-contracts | (whole repo) | searched; no settlement-cycle assumption |
