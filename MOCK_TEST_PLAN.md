# Mock Test Plan — `account-service` against a mocked `people-service`

**Status:** strategy / planning only. No mock or test code is added by this document.
**Goal:** test `account-service` (Java 21 / Spring Boot 3.3) in isolation, without running the
`.NET 8` `people-service`, and without changing production logic to special‑case tests.

---

## 1. Dependency to mock and the injection seam

### The dependency
`account-service` depends on `people-service` over plain HTTP. The only call is a person‑existence
check made before an account↔user mapping is created.

| Item | Value |
| --- | --- |
| Dependency | `people-service` (.NET 8 — a different runtime we do not want to run) |
| Call site | `AccountUserController.validatePerson(String username)` — `account-service/src/main/java/finos/traderx/accountservice/controller/AccountUserController.java:69-87` |
| Triggered by | `POST /accountuser/` → `createAccountUser()` calls `validatePerson()` (`AccountUserController.java:49-57`) |
| Endpoint called | `GET {people.service.url}/People/GetPerson?LogonId={username}` (`AccountUserController.java:70`) |
| Response type | `Person` JSON, deserialized into `finos.traderx.accountservice.model.Person` (`AccountUserController.java:74`) |
| HTTP client | `private RestTemplate restTemplate = new RestTemplate();` (`AccountUserController.java:35`) |

### The injection seams (how we swap real → mock)

1. **Config seam (primary, zero production change): `people.service.url`.**
   `AccountUserController` reads `@Value("${people.service.url}")` (`AccountUserController.java:40-41`).
   The property is defined in `account-service/src/main/resources/application.properties:15`:
   ```
   people.service.url=${PEOPLE_SERVICE_URL:http://${PEOPLE_SERVICE_HOST:localhost}:18089}
   ```
   Pointing this property (via `PEOPLE_SERVICE_URL`, a Spring property, or a test
   `@DynamicPropertySource`) at a local Prism/WireMock stub redirects every call to the mock.
   This is the seam used for both local dev and out‑of‑process integration tests. It changes **no
   production code**.

2. **`RestTemplate` bean seam (for in‑process unit tests).**
   `MockRestServiceServer` must bind to the exact `RestTemplate` instance under test. Today that
   instance is a hard `new RestTemplate()` field, which a test can only reach by reflection.
   The follow‑up session should promote it to a Spring‑managed, constructor‑injected
   `RestTemplate` bean (built via `RestTemplateBuilder`). This is a **testability refactor with no
   behavioral change** — it does not special‑case tests, it just makes the collaborator injectable
   and lets us configure connect/read timeouts (see §3, timeout scenario). If we prefer to avoid
   any production edit, the fallback is reflection to grab the private field; the bean refactor is
   the cleaner, recommended option and is called out explicitly here rather than done silently.

> **Note (existing broken test):** `AccountServiceApplicationTests.java` currently lives under
> `account-service/src/main/test/java/...` (not `src/test/java`), so Gradle never compiles or runs
> it, and it imports a non‑existent package `com.ms.sdx.accountservice.*`. The follow‑up session
> must place new tests under the standard `src/test/java` and relocate/fix this file. Treat this as
> a finding, not a place to copy conventions from.

---

## 2. Mocking approach and justification

Three complementary layers, each chosen for what it does best. This mirrors the repo's existing
mocking convention (every Java service README documents mocking with `@stoplight/prism-cli` off its
own `openapi.yaml` — e.g. `account-service/README.md:48-60`, `position-service/README.md:50-61`,
`trade-service/README.md:44-55`, `reference-data/README.md:74-87`).

| Layer | Tool | Scope | Why |
| --- | --- | --- | --- |
| A. Contract stub | **Prism** (`@stoplight/prism-cli`) off `people-service/openapi.yaml` | Local dev + happy path | Reuses the repo's established convention; serves spec‑derived responses so the stub cannot invent shapes the contract forbids. Started on `18089` so the default `people.service.url` "just works". |
| B. Failure injection | **WireMock** (pure‑JVM) | CI integration tests | Prism can't easily produce connection‑refused, arbitrary delays, 5xx, or malformed bodies. WireMock is a JVM library (no Node, no .NET), starts on a random port, and is wired in via `@DynamicPropertySource` → `people.service.url`. This is the primary engine for failure‑mode tests and keeps CI fully offline. |
| C. In‑process | **`MockRestServiceServer`** (spring‑test) | Fast unit tests of `validatePerson` | No sockets, deterministic, millisecond‑fast; binds to the `RestTemplate` bean (§1, seam 2). Best for asserting the exact URL built and the branch logic. |

**Why not just Prism for everything:** Prism is great for happy path and contract conformance but
weak at simulating transport‑level failures (refused/timeout) and malformed responses. **Why not
just WireMock:** WireMock stubs are hand‑written and can drift from the real contract — which is why
Prism (spec‑driven) plus the contract‑conformance check in §4 guard it.

---

## 3. Scenarios to cover

`validatePerson` returns `true`/`false`; `createAccountUser` maps `false` → `ResourceNotFoundException`.
Exception handling: `@ExceptionHandler(ResourceNotFoundException.class)` → **404** (body
`"{username} not found in People service."`); `@ExceptionHandler(Exception.class)` → **500**
(`AccountUserController.java:89-97`). Note the `catch` block only catches `HttpClientErrorException`
(`AccountUserController.java:78`), so anything else propagates to the general 500 handler.

| # | Scenario | Mock setup | Expected graceful behavior of `account-service` | Current actual behavior (to assert / flag) |
| --- | --- | --- | --- | --- |
| S1 | **Success** — person exists | 200 + valid `Person` JSON | `validatePerson`→`true`; `POST /accountuser/` → **200** with persisted `AccountUser` | Matches. ✅ |
| S2 | **404** — person not found | 404 | `validatePerson`→`false`; **404** with body `"{username} not found in People service."` | Matches (`HttpClientErrorException` 404 branch). ✅ |
| S3 | **400** — bad request | 400 | `false` → **404 not found** (person treated as absent) | Matches, but conflates "bad request" with "not found". Flag as finding. ⚠️ |
| S4 | **500** — dependency error | 500 | Ideally a distinct **502/503** ("dependency unavailable"), not a generic 500 | `HttpServerErrorException` is **not** caught → bubbles to `Exception` handler → **500**. Test documents current 500; flag gap. ⚠️ |
| S5 | **Dependency unavailable** (connection refused) | `people.service.url` pointed at a dead port / WireMock stopped | Ideally **502/503** with a clear "people-service unreachable" message | `ResourceAccessException` not caught → **500**. Test documents 500; flag gap. ⚠️ |
| S6 | **Timeout / slow** | WireMock fixed delay (e.g. 5s) with a short client read‑timeout | Fail fast → **502/503** within the timeout budget | `new RestTemplate()` sets **no timeouts** → request hangs indefinitely. Test requires the RestTemplate‑bean refactor (§1) to set timeouts, then asserts fail‑fast. Flag gap. ⚠️ |
| S7 | **Malformed / empty body** | 200 with non‑JSON, or 200 empty body | Fail safe → **404** (treat as invalid) or **502**, no crash | Empty/`null` body → `response.getBody().toString()` NPE; non‑JSON → `RestClientException`; neither is `HttpClientErrorException` → **500**. Flag gap. ⚠️ |

The `⚠️` rows are the value of this exercise: the mock lets us reproduce every failure mode offline
and shows that `account-service` currently degrades to a blunt 500 (or hangs, S6). Tests will assert
**current** behavior and each `⚠️` becomes a documented finding for a later hardening PR — no
production change is made under this plan.

---

## 4. Contract‑conformance check (mock cannot drift from the spec)

Two guards keep every stubbed response faithful to `people-service/openapi.yaml`:

1. **Spec‑driven happy path (Prism, layer A).** Because Prism generates responses *from* the
   OpenAPI document, the success stub's shape is derived from the contract by construction.

2. **Response validation on WireMock (layer B).** Attach the Atlassian
   `swagger-request-validator-wiremock` `OpenApiValidationListener`, loading
   `people-service/openapi.yaml`, to the WireMock server. Any stubbed response that violates the
   spec (wrong status for the path, wrong `Person` shape, missing required content) fails the test.
   This means a hand‑written WireMock stub *cannot* silently diverge from the real contract.

3. **CI drift gate.** A CI step runs Prism in validation mode against the same
   `people-service/openapi.yaml` and the test asserts requests/responses validate. If
   `people-service/openapi.yaml` changes and the stubs are not updated, the contract test fails,
   surfacing drift immediately.

The single source of truth for all three is the checked‑in `people-service/openapi.yaml`
(the `Person` schema at lines 85‑108, and the `GET /People/GetPerson` operation at lines 6‑32).

---

## 5. Offline / deterministic constraints and CI outline

**Constraints**
- **No .NET runtime** anywhere in the account‑service test job — `people-service` is never built or run.
- **No network egress.** WireMock (layer B) and `MockRestServiceServer` (layer C) are in‑JVM; the
  DB is in‑memory H2 (`test-application.properties` → `jdbc:h2:mem:test`), so no external DB either.
  Prism (layer A) is documented for *local* dev; CI relies on the pure‑JVM layers to avoid needing Node.
- **Deterministic:** fixed stub bodies (spec examples), ephemeral ports resolved via
  `@DynamicPropertySource`, no wall‑clock/random dependencies, delays used only for the S6 timeout test.

**CI outline** (new job, e.g. `.github/workflows/account-service-test.yml`, or a `test` job added to
`build-and-publish.yml`):
```yaml
jobs:
  account-service-test:
    runs-on: ubuntu-latest          # no .NET SDK installed / used
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - name: Run account-service tests (mocked people-service)
        working-directory: account-service
        run: ./gradlew test          # WireMock + MockRestServiceServer + in-mem H2; fully offline
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: account-service-test-report, path: account-service/build/reports/tests/test }
```
No service containers, no `people-service` image, no Node required for the gate.

---

## 6. Files the follow‑up implementation session will add / change

Under `account-service/`:

- **`build.gradle`** — add test deps:
  `org.wiremock:wiremock-standalone` and
  `com.atlassian.oai:swagger-request-validator-wiremock` (both `testImplementation`).
- **`src/main/java/finos/traderx/accountservice/config/RestClientConfig.java`** *(optional, recommended)* —
  a `@Bean RestTemplate` (via `RestTemplateBuilder` with connect/read timeouts); change
  `AccountUserController` to constructor‑inject it. Testability refactor only (enables S6 + layer C).
- **`src/test/java/finos/traderx/accountservice/controller/AccountUserControllerMockRestTest.java`** —
  layer C: `MockRestServiceServer` unit tests for S1–S3, S7 and exact URL assertion.
- **`src/test/java/finos/traderx/accountservice/controller/AccountUserControllerWireMockIT.java`** —
  layer B: `@SpringBootTest(RANDOM_PORT)` + WireMock + `@DynamicPropertySource` for S1, S4, S5, S6, S7.
- **`src/test/java/finos/traderx/accountservice/contract/PeopleServiceContractTest.java`** —
  §4 conformance: WireMock + `OpenApiValidationListener` loading `people-service/openapi.yaml`.
- **`src/test/resources/people-service-openapi.yaml`** — copy of / build‑time reference to
  `../../people-service/openapi.yaml` so tests read the contract deterministically.
- **Relocate/fix** `src/main/test/java/.../AccountServiceApplicationTests.java` → `src/test/java/...`
  with the correct `finos.traderx.accountservice` imports (see §1 note).
- **`mock-people-service.sh`** *(optional)* — one‑liner
  `prism --cors --port 18089 mock ../people-service/openapi.yaml` for local dev (layer A).

At repo root:
- **`.github/workflows/account-service-test.yml`** *(or a `test` job in `build-and-publish.yml`)* — the §5 CI gate.
