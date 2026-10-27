# Coverage Improvement Plan

_Measured on 2026-07-16 against `main`. Numbers below come from running each
package's own coverage tooling locally; backend numbers were obtained with
temporary, uncommitted tooling changes used only to measure (see
[Prerequisites to make coverage measurable](#prerequisites-to-make-coverage-measurable))._

## TL;DR

The overwhelming majority of the platform is untested. Every backend service
(the five Java services and the .NET `people-service`) reports **0% measured
line coverage** because no test actually executes today, and the React
front-end and `trade-feed` are also at **0%**. Only three packages run
meaningful tests: `reference-data` (~63% lines), the Angular front-end
(~52% lines, but only after fixing two compile errors that currently break its
test build), and — barely — the React CLI stub.

The **trade path is the highest business risk**: `trade-service`
(trade-order entry via `TradeOrderController`), `trade-processor`
(async trade matching / feed handler), and `trade-feed` (the Socket.IO event
bus) carry the most business-critical logic **and** are almost entirely
uncovered. They should be remediated first.

## How coverage is measured today

| Package | Language / Stack | Coverage tool wired up? | Command |
|---|---|---|---|
| `reference-data` | NestJS (Node/TS) | **Yes** — Jest (`--coverage`), `collectCoverageFrom: ["**/*.(t\|j)s"]`, report to `../coverage` | `npm run test:cov` |
| `web-front-end/angular` | Angular 18 | **Yes** — Karma + `karma-coverage` (`--code-coverage`), lcov + text-summary | `npm run test:ci` |
| `web-front-end/react` | CRA (react-scripts) | **Partial** — `react-scripts test` exists, but no coverage script; coverage only via ad-hoc flags | `CI=true npx react-scripts test --coverage --watchAll=false` |
| `account-service` | Java 21 / Spring Boot 3 | **No** — no JaCoCo plugin | (would be `./gradlew test jacocoTestReport`) |
| `position-service` | Java 21 / Spring Boot 3 | **No** — no JaCoCo plugin | " |
| `trade-service` | Java 21 / Spring Boot 3 | **No** — no JaCoCo plugin | " |
| `trade-processor` | Java 21 / Spring Boot 3 | **No** — no JaCoCo plugin | " |
| `database` | Java (H2 console launcher) | **No** — no Java source of its own | n/a |
| `people-service` | .NET 8 | **No** — no test project, no coverlet | n/a |
| `trade-feed` | Node.js / Socket.IO | **No** — `test` script is `echo "Error: no test specified" && exit 1` | n/a |

Other observations:

- **No JaCoCo (Java)** and **no coverlet (.NET)** are configured anywhere, so
  the backend has no coverage instrumentation at all.
- **No SonarQube configuration lives in the repo** — there is no
  `sonar-project.properties` and no `sonar-scanner` step in
  `.github/workflows/` (only `build-and-publish.yml`,
  `license-scanning-node.yml`, and `security.yml`). Any SonarQube quality gate
  is therefore configured server-side and cannot read these reports until the
  repo publishes them (see Prerequisites).
- **The Java tests do not run.** In `account-service`, `position-service`, and
  `trade-processor` the only test file lives under the non-standard
  `src/main/test/java/...` path, which Gradle's default `test` source set
  (`src/test/java`) never picks up — so `./gradlew test` reports
  `test NO-SOURCE` and `jacocoTestReport SKIPPED`. Worse, all three are the
  **same copy-pasted `AccountServiceApplicationTests.java`** importing
  `com.ms.sdx.accountservice.*`, which does not exist in this repo; relocating
  them to `src/test/java` produces `package com.ms.sdx.accountservice ... does
  not exist` compile errors. `trade-service` and `database` have **no test
  files at all**.
- **The Angular test build does not compile as-is.** `npm run test:ci` fails
  with two TypeScript errors: `assign-user.component.ts` (a strict-null
  `OperatorFunction<string, ...>` vs `string | undefined` mismatch in
  `switchMap`) and `trade-blotter.component.spec.ts` (references
  `getRowNodeId`, which was renamed to `getRowId`). The ~52% number below was
  obtained after making those two minimal fixes locally.

## Measured coverage & ranking

Ranked primarily by **uncovered lines (desc)**, with **business criticality**
as the tiebreaker. For the backend, coverage is a measured **0%**, so
"uncovered lines" is the module's source-line size (JaCoCo/coverlet emit no
report because no test executes); those rows are marked _est._

| # | Module | Stack | Line coverage | Uncovered lines | Business criticality | Why |
|---|---|---|---|---|---|---|
| 1 | `trade-processor` | Java | **0%** (measured) | ~670 _est._ | High (trade path) | No test runs (misplaced/broken copy-paste test) |
| 2 | `trade-service` | Java | **0%** (measured) | ~550 _est._ | **Highest** (trade-order entry) | No test files exist |
| 3 | `people-service` | .NET | **0%** | ~390 _est._ | Medium (identity) | No test project |
| 4 | `account-service` | Java | **0%** (measured) | ~280 _est._ | Medium | Single test is misplaced + broken imports |
| 5 | `position-service` | Java | **0%** (measured) | ~230 _est._ | Medium-High | No test runs (broken copy-paste test) |
| 6 | `web-front-end/react` | CRA | **0%** (0/188) | 188 | Medium (UI) | Only a failing CRA `App.test.tsx` stub |
| 7 | `web-front-end/angular` | Angular | **52.3%** (161/308) | 147 | Medium (UI) | Real specs exist; test build currently broken |
| 8 | `trade-feed` | Node | **0%** | ~72 _est._ | High (event bus) | No tests, no harness |
| 9 | `reference-data` | NestJS | **62.9%** (39/62) | 23 | Lower (static data) | Best-covered package |
| — | `database` | Java/H2 | n/a | n/a | Low | Launcher only; no own source |

Detailed metrics for the three packages whose tests actually run:

| Module | Statements | Branches | Functions | Lines | Tests |
|---|---|---|---|---|---|
| `reference-data` | 60.27% | 42.85% | 92.85% | 62.90% (39/62) | 14 passing |
| `web-front-end/angular` | 52.25% (174/333) | 35% (21/60) | 44% (66/150) | 52.27% (161/308) | 23 pass / 2 fail (after local compile fixes) |
| `web-front-end/react` | 0% | 0% | 0% | 0% (0/188) | 1 suite, fails to run |

### Recommended remediation order (business-criticality weighted)

Because a raw line-count ranking under-weights how critical the trade path is,
tackle the work in this order:

1. **`trade-service`** — trade-order entry, the front door of the platform.
2. **`trade-processor`** — asynchronous trade matching / feed handling.
3. **`trade-feed`** — the Socket.IO bus every trade event flows through.
4. **`position-service`** — position/holdings correctness.
5. **`people-service`** — identity and account linkage.
6. **`account-service`** — account metadata.
7. **`reference-data`** — close the remaining gaps.
8. **Front-ends** (`angular`, `react`) — fix the broken test builds, then add
   meaningful component/service tests.

## Work broken into module-sized chunks

Proposed targets: **80%** line coverage for business-critical backend services,
**70%** for the front-ends, and **80% on new code** to match SonarQube's
default "Clean as You Code" new-code coverage gate (the repo has no discoverable
gate config, so 80%-on-new-code is the sensible default to adopt).

### 1. `trade-service` (Java) — 0% → **80%**
- Prereq: add JaCoCo; create `src/test/java`.
- Unit-test `TradeOrderController`: request validation (missing/invalid
  security, quantity, side, account) and routing/publish of accepted orders.
- Unit-test `SocketIOJSONPublisher`: JSON serialization of the envelope and
  publish behavior, **mocking the Socket.IO client** so no live socket is
  needed.
- Cover the `model` classes (`TradeOrder`, `TradeRequest`, `TradeResponse`,
  `TradeSide`, `TradeState`) and `ResourceNotFoundException` handling.

### 2. `trade-processor` (Java) — 0% → **80%**
- Prereq: add JaCoCo; move/replace the broken `AccountServiceApplicationTests`
  into `src/test/java` with correct `finos.traderx...` packages.
- Test the trade-feed consumer/handler (`TradeFeedHandler`,
  `tradeprocessor.service.TradeService`, `TradeServiceController`): processing
  of inbound trade messages, position updates, and error paths.
- Test the `messaging.socketio` subscriber/publisher with a mocked socket.

### 3. `trade-feed` (Node/Socket.IO) — 0% → **70%**
- Add a test harness first (currently only `index.js` + `package.json`, and the
  `test` script just errors). Add Jest (or node:test) + a `test:cov` script.
- Test message relay and socket events: client connect/subscribe, event
  fan-out/broadcast, and CORS/route wiring in the Express app.

### 4. `position-service` (Java) — 0% → **80%**
- Prereq: add JaCoCo; **replace** the broken copy-pasted
  `AccountServiceApplicationTests` (wrong package/imports) under
  `src/main/test` with real tests in `src/test/java`.
- Test `PositionService`/`TradeService` and `PositionController`/`TradeController`:
  position aggregation from trades, lookups by account, and not-found handling.

### 5. `people-service` (.NET) — 0% → **80%**
- Prereq: create an xUnit test project (e.g. `PeopleService.Tests`) referencing
  `PeopleService.Core` and `PeopleService.WebApi`, add **coverlet**
  (`coverlet.collector`) to it, and add it to `PeopleService.sln`.
- Test the domain/services in `PeopleService.Core` and the WebApi controllers
  (person lookup, account linkage, error responses).

### 6. `account-service` (Java) — 0% → **80%**
- Prereq: add JaCoCo; move tests to `src/test/java` and fix imports to
  `finos.traderx.accountservice.*`.
- Expand well beyond the single `AccountServiceApplicationTests`: cover
  `AccountService`, `AccountUserService`, `AccountController`,
  `AccountUserController`, and repository/model edge cases.

### 7. `reference-data` (NestJS) — 62.9% → **80%**
- Already has substantive specs for `stocks.*`, `health.*`, and the CSV loader.
- Fill gaps beyond `stocks.*`: `app.module.ts`, `main.ts` bootstrap,
  `health.module.ts`, and `stocks.module.ts` wiring (currently 0% each).

### 8. `web-front-end/angular` — 52.3% → **70%**
- **First fix the broken test build** (the two TS errors above) so
  `npm run test:ci` compiles and the two currently-failing `trade-blotter`
  specs pass.
- Replace CLI-generated stubs with meaningful tests, prioritizing the least
  covered: `trade-feed.service.ts` (5%), `trade-ticket.component.ts` (6%),
  `account.service.ts` (6%), `position-blotter.component.ts` (27%), and the
  other service classes (`user`/`symbols`/`position` services at ~12%).

### 9. `web-front-end/react` — 0% → **70%**
- Add a `test:cov` script (see Prerequisites) and replace the CRA-generated
  `App.test.tsx` stub (which currently fails to run) with real component/hook
  tests: `Datatable`, `AccountsDropdown`, `ActionButtons/*`, and the
  `hooks/Get*` data hooks (mock the socket/HTTP layer).

## Prerequisites to make coverage measurable

These are recommendations to implement in follow-up PRs — **none are applied in
this PR**, which contains only this document.

1. **Add JaCoCo to the five Java `build.gradle` files**
   (`account-service`, `position-service`, `trade-service`, `trade-processor`,
   and — if it gains tests — `database`): apply `id 'jacoco'`, wire
   `test { finalizedBy jacocoTestReport }`, and enable the XML report so a
   scanner can read it.
2. **Move the Java tests to `src/test/java`** with correct
   `finos.traderx.*` packages (the current `src/main/test/java` files with
   `com.ms.sdx.accountservice.*` imports are dead code that never compiles or
   runs).
3. **Add a test project + coverlet to `people-service`**: create
   `PeopleService.Tests` (xUnit), reference `coverlet.collector`, and add it to
   `PeopleService.sln` so `dotnet test --collect:"XPlat Code Coverage"`
   produces a report.
4. **Add a `test:cov` script to `web-front-end/react`** (e.g.
   `"test:cov": "CI=true react-scripts test --coverage --watchAll=false"`) so
   coverage is a first-class, repeatable command like the other Node packages.
5. **Add a test harness to `trade-feed`** (Jest or `node:test`) with a
   `test`/`test:cov` script, replacing the placeholder `test` that just errors.
6. **Fix the Angular test build** so `npm run test:ci` compiles (the two TS
   errors above), otherwise its coverage cannot be produced in CI.
7. **(Optional) Commit a `sonar-project.properties`** that points SonarQube at
   each package's report path (JaCoCo XML, coverlet Cobertura/OpenCover, Jest
   lcov, Karma lcov) so the server-side quality gate can actually read coverage
   and enforce the new-code threshold.
