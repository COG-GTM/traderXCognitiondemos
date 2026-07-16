# TraderX Integration Testing Strategy

> **Status:** Plan only. This document defines *what* integration tests we will add and *how*;
> no test code is added in this change. Implementation follows in a dedicated session
> (see [Section 6](#6-implementation-backlog-files--modules)).

## 0. Why this exists

TraderX is a polyglot, distributed trading platform. Its correctness lives almost entirely
**at the seams between services** — REST calls between Spring services, the `socket.io`
trade-feed bus, and a shared H2 database — yet today those seams have **zero automated
coverage**.

What exists today (verified by reading the repo):

| Area | Tests present | Notes |
| --- | --- | --- |
| `reference-data` (NestJS) | 4 Jest specs (`*.spec.ts`) | unit-level, service/controller |
| `web-front-end/angular` | 9 Karma/Jasmine specs (`*.spec.ts`) | component unit tests; trade-feed is **mocked** |
| `account-service`, `position-service`, `trade-processor` (Java) | 1 stale file each | see below |
| `trade-service` (Java) | **none** | no test directory at all |
| `people-service` (.NET) | **none** | — |
| **Service-to-service integration** | **none** | **the gap this plan closes** |

The three Java "tests" are all identical copies of `AccountServiceApplicationTests.java` and are
**not actually run**:

1. They live under `src/main/test/java/...` — the wrong Gradle source set. The `test` task
   only compiles `src/test/java`, so Gradle never sees them.
2. The `account-service` copy imports `com.ms.sdx.accountservice.model.Account` /
   `...service.AccountService`, packages that **do not exist** in this repo — it would not
   compile even if relocated.

**Net effective integration-test count today: 0.** Relocating/fixing these unit tests is a
prerequisite hygiene step but is out of scope for the integration effort itself.

---

## 1. Seam map (producer → consumer), ranked by risk

Ports and wiring below are taken from `docker-compose.yml` and each service's
`application.properties`.

| # | Producer → Consumer | Transport | Data crossing the seam | Contract source | Risk | Tested today |
| --- | --- | --- | --- | --- | --- | --- |
| **S1** | **`trade-service` → `trade-feed`** | REST in (`POST /trade/`) → **socket.io** publish to topic `/trades` | `TradeOrder` {id, security, quantity, accountID, side, state} | `trade-service/openapi.yaml` | 🔴 **Critical** | ❌ |
| **S2** | **`trade-feed` → `trade-processor`** | **socket.io** subscribe `/trades` → **H2 SQL write** → socket.io publish `/accounts/{id}/trades`, `/accounts/{id}/positions` | `TradeOrder` in; `Trades` + `Positions` rows out; `Trade`/`Position` events out | `trade-processor/openapi.yaml`, `database/initialSchema.sql` | 🔴 **Critical** | ❌ |
| **S3** | **`database` → `position-service`** | **H2 SQL read** (JPA) → REST out (`GET /trades/{id}`, `/positions/{id}`) | `Trade[]`, `Position[]` | `position-service/openapi.yaml` | 🟠 **High** | ❌ |
| **S4** | `trade-service` → `account-service` | REST (`GET /account/{id}`) | `Account` (validate account exists) | `account-service/openapi.yaml` | 🟠 **High** | ❌ |
| **S5** | `trade-service` → `reference-data` | REST (`GET /stocks/{ticker}`) | `Security` (validate ticker exists) | `reference-data/openapi.yaml` | 🟠 **High** | ❌ |
| **S6** | `account-service` → `people-service` | REST (`GET /People/GetPerson?LogonId=`) | `Person` (validate user exists) | `people-service/openapi.yaml` | 🟡 **Medium** | ❌ |
| **S7** | `database` → `account-service` | **H2 SQL** (JPA CRUD) | `Accounts`, `AccountUsers` | `account-service/openapi.yaml` | 🟡 **Medium** | ❌ |
| **S8** | `trade-feed` → `web-front-end` | **socket.io** subscribe `/accounts/{id}/trades`, `/accounts/{id}/positions` | live `Trade`/`Position` payloads | front-end services | 🟡 **Medium** | partial (mocked) |
| **S9** | `web-front-end` → REST APIs | REST clients | `Account`, `AccountUser`, `Trade`, `Position`, `Stock`, trade ticket | all `openapi.yaml` | 🟢 **Low** | partial (mocked) |

### The critical path: the asynchronous order flow (S1 → S2 → S3)

```
web-front-end                 trade-service                 trade-feed              trade-processor                 database                position-service
     │  POST ticket  ───────────▶ │                             │                       │                            │                          │
     │                            │ validateTicker (S5) ───▶ reference-data             │                            │                          │
     │                            │ validateAccount (S4) ───▶ account-service           │                            │                          │
     │                            │ publish TradeOrder ───────▶ │  topic "/trades" (S1) │                            │                          │
     │                            │                             │ ──── deliver ───────▶ │ processTrade()             │                          │
     │                            │                             │                       │ INSERT Trades / UPSERT Positions (S2) ─▶ │            │
     │                            │                             │ ◀── publish Trade ──── │                            │                          │
     │ ◀ /accounts/{id}/trades (S8) ◀──────────────────────────│                       │                            │                          │
     │                            │                             │                       │                            │ ◀── GET /positions (S3)  │
```

This chain is **highest risk** because it is the core product behaviour, is fully
asynchronous (fire-and-forget across `socket.io`), performs the only DB *writes* in the
system, and has **no test at any level**. It is where a regression is most likely and most
expensive.

### Seam behaviours the tests must pin (found in code, easy to regress)

- **Double-slash URLs.** `TradeOrderController` builds `.../ /stocks/{t}` and `.../ /account/{id}`
  (literal `//`). `AccountUserController` calls `/People/GetPerson?LogonId=`. Stubs must match
  these exact paths.
- **Fail-closed validation, but only on `HttpClientErrorException`.** `validateTicker` /
  `validateAccount` return `false` for a 404 (→ `404 ResourceNotFoundException` to caller) but a
  5xx / connection error bubbles up as a `500`. Tests must cover both 404 and 5xx.
- **No trade is published if validation fails** — assert the trade-feed received *nothing*.
- **Position math & lifecycle.** `TradeService.processTrade` computes
  `qty * (Buy? +1 : -1)`, creates a position at 0 when absent, and drives state
  `New → Processing → Settled`, persisting `Trade` twice.
- **DB constraints from `initialSchema.sql`.** `Quantity > 0`, `Side ∈ {Buy,Sell}`,
  `State ∈ {New,Processing,Settled,Cancelled}`, FK `Trades.AccountID → Accounts.ID`.

---

## 2. Test approach per seam

Global recipe for the Java services (Spring Boot 3.3, Java 21):

- **Boot the real service** with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and a
  `TestRestTemplate`/`WebTestClient` so the full web stack (controllers, serialization,
  exception handlers) is exercised.
- **Real database.** Default to the **built-in H2** the services already use, initialized from
  the real `database/initialSchema.sql` so fixtures and constraints match production. Provide a
  **Testcontainers** variant (see [Section 4](#4-deterministic--offline-constraints)) for teams
  that want engine parity; the schema/seed source is identical either way.
- **Stub neighbours, never boot them.** Neighbour services (`reference-data`,
  `account-service`, `people-service`) are replaced with **WireMock** (out-of-process, real
  HTTP — preferred for `RestTemplate` callers) or `MockRestServiceServer` (in-process). Stub
  responses are **generated from / validated against the neighbour's `openapi.yaml`** so a
  stub can never drift from the real contract.
- **Assertions:** (a) HTTP response contract (status + body shape vs the *producer's*
  `openapi.yaml`), **and** (b) the resulting side effect — persisted rows and/or produced
  trade-feed messages.

| Seam | Boot | Real DB | Stubbed neighbour(s) | Key assertions |
| --- | --- | --- | --- | --- |
| **S1** | `trade-service` | n/a | `reference-data` + `account-service` (WireMock, happy path); **embedded trade-feed** as the publish target | `POST /trade/` returns `200` + `TradeOrder`; **exactly one** message on `/trades` with matching payload; invalid ticker/account → `404` and **no** message published |
| **S2** | `trade-processor` | ✅ (H2, seeded) | none (subscribes to embedded trade-feed) | after injecting a `TradeOrder` on `/trades`: `Trades` row exists with `state=Settled`; `Positions` upserted by `±qty`; a `Trade` msg on `/accounts/{id}/trades` and a `Position` msg on `/accounts/{id}/positions` |
| **S3** | `position-service` | ✅ (H2, seeded) | none | `GET /trades/{id}` & `/positions/{id}` return arrays matching `position-service/openapi.yaml`; `{id}` with no data → empty array; body matches seeded fixtures |
| **S4** | `trade-service` | n/a | `account-service` (WireMock) | 200 account → order proceeds; 404 → `404` to caller; 5xx → `500` |
| **S5** | `trade-service` | n/a | `reference-data` (WireMock) | 200 ticker → proceeds; 404 → `404`; verify exact `//stocks/{ticker}` path called |
| **S6** | `account-service` | ✅ (H2) | `people-service` (WireMock) | `POST /accountuser/` with known person → `200` + row persisted; unknown → `404`, **no** row |
| **S7** | `account-service` | ✅ (H2, seeded) | none | account/account-user CRUD round-trips; responses match `account-service/openapi.yaml`; FK + PK constraints enforced |
| **S8/S9** | front-end | n/a | see [Section 3](#3-contract-tests) | consumer contract check (below) |

### The end-to-end order-flow test (S1+S2 across a real bus)

A single "narrow E2E" test wires **`trade-service` + real/embedded `trade-feed` + `trade-processor` + H2**
together (neighbours S4/S5 stubbed with WireMock), then:

1. `POST /trade/` a valid `TradeOrder` to `trade-service`.
2. **Await** (bounded poll) until `trade-processor` has persisted the `Trades`/`Positions` rows.
3. Assert the row values, the `Positions` delta, and the two outbound trade-feed messages.

**Asynchrony rule: no `Thread.sleep`.** All waits use **bounded polling** with a hard timeout,
via **Awaitility**:

```java
await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100))
       .untilAsserted(() -> assertThat(tradeRepository.findByAccountId(ACCT)).hasSize(1));
```

Trade-feed options, in order of preference:
- **Embedded Node `trade-feed`** started via Testcontainers (`node:` image running `trade-feed/index.js`) — real bus, real serialization.
- A lightweight in-test `socket.io` server implementing the same `subscribe`/`publish`
  envelope contract (`{type, from, topic, date, payload}`) for pure-JVM runs.

---

## 3. Contract tests

Two directions, both driven by the `openapi.yaml` files (the stated source of truth):

1. **Provider contract (per Java service).** Validate each service's live responses against its
   own `openapi.yaml` using an OpenAPI request/response validator
   (`swagger-request-validator-restassured` / `atlassian swagger-request-validator`). Wrap the
   `TestRestTemplate`/RestAssured calls so **every** integration test above also asserts schema
   conformance for free. Covers `trade-service`, `account-service`, `position-service`,
   `trade-processor`.
   - For `reference-data` (NestJS) and `people-service` (.NET), add a provider check that the
     running service's responses match their `openapi.yaml` (Jest + a schema validator for
     NestJS; a schema-assertion test for .NET — lower priority, tracked as follow-up).

2. **Consumer contract (web front-end).** Assert the Angular API clients call the paths/shapes
   the providers actually expose. Concretely, cross-check the URLs/DTOs in
   `account.service.ts`, `position.service.ts`, `symbols.service.ts`, `trade-feed.service.ts`
   against the corresponding `openapi.yaml` (and the trade-feed topic names
   `/accounts/{id}/trades`, `/accounts/{id}/positions`). Implemented as a Karma/Jasmine spec
   using `HttpTestingController` that fails if a client path diverges from the OpenAPI contract.

This gives us **provider-verified** contracts on the server side and a **consumer-driven**
check on the client side, meeting in the middle at each `openapi.yaml`.

---

## 4. Deterministic / offline constraints

- **No shared or cloud environment.** Every test spins up its own dependencies in-process or
  via Testcontainers. No test may touch the compose stack, a shared H2 TCP server, or any
  remote host.
- **Seeded, fixed reference data.** Reuse `database/initialSchema.sql` (accounts `22214`,
  `52355`, etc., and the sample trades/positions) as the canonical fixture so assertions use
  known IDs. Tests that mutate data create their own accounts/securities and clean up (or run
  in a rolled-back transaction) to stay order-independent.
- **Deterministic time & IDs.** `TradeService` uses `new Date()` and `UUID.randomUUID()`;
  tests assert on *relationships and presence*, not exact timestamps/ids (or inject a fixed
  `Clock`/id supplier in a follow-up refactor).
- **Bounded async.** Awaitility timeouts (≤10s) everywhere; **`Thread.sleep` is banned** and
  should be enforced by a lint/Checkstyle rule.
- **Stubs pinned to contracts.** WireMock stubs are validated against the neighbour's
  `openapi.yaml` at test-setup time so an upstream contract change breaks the stub loudly.
- **Hermetic ports.** `RANDOM_PORT` for booted services; Testcontainers-assigned ports for the
  bus/DB — never the fixed `1808x` ports from compose.

### CI workflow outline

New workflow `.github/workflows/integration-tests.yml` (complements the existing
`build-and-publish.yml`), triggered on PR + push to `main`:

```yaml
name: Integration Tests
on: { pull_request: {}, push: { branches: [ main ] } }
jobs:
  java-integration:
    runs-on: ubuntu-latest          # Docker available for Testcontainers
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4   # temurin 21
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew integrationTest --no-daemon   # new source set, see Section 5
      - uses: actions/upload-artifact@v4              # JUnit + OpenAPI-validation reports
  frontend-contract:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: cd web-front-end/angular && npm ci && npm run test -- --watch=false --browsers=ChromeHeadless
```

A dedicated Gradle `integrationTest` task/source set (`src/integrationTest/java`) keeps slow,
Docker-dependent tests separate from unit tests and lets CI run them as their own gate.

---

## 5. Structural fixes required before/with implementation

These are small, mechanical prerequisites (tracked, not done here):

1. **Move the stray unit tests** from `src/main/test/java` → `src/test/java` in
   `account-service`, `position-service`, `trade-processor`, and **fix the broken
   `com.ms.sdx.*` imports** so they compile and run.
2. **Add an `integrationTest` source set + Gradle task** to each Java module (or a shared
   convention plugin) so integration tests run separately from `test`.
3. **Add test-only dependencies** per module: `spring-boot-starter-test` (present),
   `org.wiremock:wiremock-standalone`, `org.awaitility:awaitility`,
   `org.testcontainers:testcontainers` (+ `junit-jupiter`),
   `swagger-request-validator-restassured`, `io.socket:socket.io-client` (already in
   `trade-service`/`trade-processor`).

---

## 6. Implementation backlog (files / modules)

Exact files to be added in the follow-up implementation session:

**trade-service** (`src/integrationTest/java/finos/traderx/tradeservice/`)
- `TradeOrderControllerIT.java` — S1: POST contract + publish-to-`/trades` (embedded feed), happy path.
- `TradeOrderValidationIT.java` — S4/S5: reference-data + account-service stubbed (200/404/5xx); no-publish-on-failure.
- `TradeServiceOpenApiContractIT.java` — provider contract vs `trade-service/openapi.yaml`.

**trade-processor** (`src/integrationTest/java/finos/traderx/tradeprocessor/`)
- `TradeProcessorConsumerIT.java` — S2: inject `TradeOrder` on `/trades`; assert `Trades`/`Positions` rows + outbound events.
- `PositionMathIT.java` — Buy/Sell delta, new-position creation, `New→Processing→Settled`.

**position-service** (`src/integrationTest/java/finos/traderx/positionservice/`)
- `PositionQueryIT.java` — S3: `/trades/{id}`, `/positions/{id}` against seeded H2.
- `PositionServiceOpenApiContractIT.java` — provider contract vs `position-service/openapi.yaml`.

**account-service** (`src/integrationTest/java/finos/traderx/accountservice/`)
- `AccountUserPeopleValidationIT.java` — S6: people-service stubbed (200/404); persist-only-if-valid.
- `AccountCrudIT.java` — S7: account/account-user CRUD round-trip + constraints.
- `AccountServiceOpenApiContractIT.java` — provider contract vs `account-service/openapi.yaml`.

**End-to-end (new top-level module `integration-tests/` or `trade-processor` IT set)**
- `OrderFlowE2EIT.java` — S1→S2 across real/embedded `trade-feed` + H2 with Awaitility.

**web-front-end/angular** (`main/app/service/`)
- `api-contract.spec.ts` — S9: client paths/DTOs vs each `openapi.yaml` via `HttpTestingController`.
- `trade-feed.contract.spec.ts` — S8: topic names + payload shape for `/accounts/{id}/trades|positions`.

**CI**
- `.github/workflows/integration-tests.yml` — as outlined in Section 4.

### Before / after target

| Metric | Before | After (target) |
| --- | --- | --- |
| Service-to-service integration tests | **0** | **~12 IT classes** covering S1–S9 |
| Seams with coverage | 0 / 9 | 9 / 9 (S8/S9 via contract level) |
| Async order-flow (S1→S2→S3) covered | ❌ | ✅ (narrow E2E + per-seam) |
| Provider OpenAPI contract checks (Java) | 0 | 4 services |
| CI integration gate | none | `integration-tests.yml` on every PR |

---

## 7. Out of scope for this plan

- Writing the test code itself (this is the strategy doc only).
- Performance/load testing of the trade-feed.
- `people-service` (.NET) and `reference-data` (NestJS) *provider* contract tests beyond the
  stub-conformance use — tracked as fast-follow.
- Full browser E2E (Cypress/Playwright) of the Angular UI.
