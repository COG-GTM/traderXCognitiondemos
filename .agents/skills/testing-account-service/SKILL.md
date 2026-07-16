---
name: Testing account-service (Spring Boot module)
description: How to build and test the traderX account-service Gradle module, including the JDK 21 requirement and how its tests mock the People Service.
---

# Testing `account-service`

`account-service` is a Spring Boot / Gradle module in `traderXCognitiondemos`.

## Build/test requirements
- Requires **JDK 21** (`build.gradle` sets `sourceCompatibility = JavaVersion.VERSION_21`). The box default `java` is 17, so builds fail unless `JAVA_HOME` points to JDK 21.
- JDK 21 is installed at `/usr/lib/jvm/java-21-openjdk-amd64`.

## Run the test suite (primary verification, shell-only)
```bash
cd account-service
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew clean test
```
- Use `clean` to force real execution instead of `UP-TO-DATE` cache.
- Expect `BUILD SUCCESSFUL` and 4 tests (2 in `AccountServiceApplicationTests`, 2 in `controller/AccountUserControllerTests`).

## Evidence locations
- JUnit XML: `account-service/build/test-results/test/*.xml` (grep for `tests=`/`failures=`/`errors=`).
- HTML report: `account-service/build/reports/tests/test/index.html` (open in browser; shows tests/failures/ignored/100%).

## How the endpoint tests work
- Gradle only runs `src/test/...` (not `src/main/test`). Tests must live under `src/test/java` and `src/test/resources`.
- `AccountUserControllerTests` starts an okhttp `MockWebServer` and registers its URL as `people.service.url` via `@DynamicPropertySource`, so `POST /accountuser/` can be tested without the real .NET People Service.
- `POST /accountuser/` validates the person by calling `people.service.url + /People/GetPerson?LogonId=<username>`: 200 → account user created (HTTP 200); 404 → `ResourceNotFoundException` → HTTP 404.
- Tests use an in-memory H2 db (`jdbc:h2:mem:test` from `src/test/resources/test-application.properties`), so no external DB/TCP H2 server is needed.

## Adversarial check that works
Temporarily flip the mocked `setResponseCode(200)` to `404` in `createAccountUserWhenPersonExists` and re-run — the test should FAIL, proving it is non-vacuous. Revert afterward.

## Notes
- No UI in this module — do not record a screen video; capture terminal output and the Gradle HTML report instead.
- A standalone live run is optional; the MockWebServer test already boots the full Spring context (Tomcat + JPA/H2 + real HTTP via `TestRestTemplate`).
