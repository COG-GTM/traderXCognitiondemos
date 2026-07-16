# Test Plan: account-service test suite (PR #62)

## Context
Backend-only PR adding/relocating the Spring Boot `account-service` test suite. No production code changed.
This is a shell-only verification (no UI) — evidence is terminal output + generated Gradle reports. No recording.

Module requires JDK 21 (`sourceCompatibility 21`). Box default `java` is 17, so must set
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.

Expected 4 tests:
- `AccountServiceApplicationTests.contextLoads`
- `AccountServiceApplicationTests.createAccount`
- `AccountUserControllerTests.createAccountUserWhenPersonExists`
- `AccountUserControllerTests.createAccountUserWhenPersonNotFound`

Key source refs:
- Controller `AccountUserController.java` L49-57 (`POST /accountuser/` -> validatePerson), L69-87 (calls `people.service.url` + `/People/GetPerson?LogonId=<user>`; 404 -> false), L89-92 (ResourceNotFound -> HTTP 404).
- Test `AccountUserControllerTests.java` uses okhttp MockWebServer registered via `@DynamicPropertySource` as `people.service.url`.

## Test 1 (Primary): Full suite passes on JDK 21
Steps:
1. `cd account-service && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew clean test --info` (clean to force actual execution, not UP-TO-DATE cache).

Pass criteria:
- Gradle prints `BUILD SUCCESSFUL`, exit code 0.
- Exactly 4 tests executed, 0 failed, 0 skipped (verified from XML result files, not just build status).
- No compilation errors (proves `com.ms.sdx.*` -> `finos.traderx.*` import fix and `mockwebserver` dependency resolve correctly).

Fail signals to watch: `BUILD FAILED`, `cannot find symbol`, `package com.ms.sdx does not exist`, `could not resolve okhttp3`, `tests completed, N failed`.

## Test 2: Verify report artifacts + per-test pass status
Steps:
1. List `build/test-results/test/*.xml` and parse each `<testsuite>` for `tests=`, `failures=`, `errors=`, `skipped=`.
2. Open `build/reports/tests/test/index.html` summary (grep for failure count / "100%" success / "0 failures").

Pass criteria:
- `AccountUserControllerTests` XML: `tests="2" failures="0" errors="0"`.
- `AccountServiceApplicationTests` XML: `tests="2" failures="0" errors="0"`.
- HTML report shows 4 tests, 0 failures, 100% successful.

## Test 3 (Adversarial): confirm the new MockWebServer test actually exercises the People-service path
Rationale: a broken/no-op test would pass identically. Prove the person-exists and person-not-found branches are genuinely driven.
Steps:
1. Re-run only the new class with detailed events:
   `JAVA_HOME=... ./gradlew test --tests 'finos.traderx.accountservice.controller.AccountUserControllerTests' -i` and inspect stdout/`build/reports`.
2. Adversarial mutation check (temporary, reverted after): temporarily change the mocked 200 response in `createAccountUserWhenPersonExists` to `setResponseCode(404)` and re-run.

Pass criteria:
- Baseline: both new tests pass; the recorded request path assertion `/People/GetPerson?LogonId=jdoe` holds (test would fail if controller/mock wiring were wrong).
- Mutation: `createAccountUserWhenPersonExists` now FAILS (expects 200 OK but gets 404), proving the assertion is real and not vacuous.
- Revert the mutation and confirm suite returns to 4/4 passing.

## Notes
- Live end-to-end run (starting the service against H2 + stubbed People service) is optional/best-effort per the task. Attempt only if time permits after the suite is proven; otherwise report gradle suite as sufficient evidence.
