# TEST_REPORT_api.md — REST API edge & corner case tests

Scope: `trade-service`, `account-service`, `position-service` HTTP APIs.
Branch: `devin/swarm-api-tests`.

Command: `./gradlew :trade-service:test :account-service:test :position-service:test --console=plain`

| Suite | Tests | Passing | Disabled (bug-exposing) |
|---|---|---|---|
| `trade-service` (`TradeOrderControllerTest`) | 43 | 35 | 8 |
| `account-service` (`AccountControllerTest`, `AccountUserControllerTest`, `AccountServiceTest`) | 49 | 39 | 10 |
| `position-service` (`PositionControllerTest`, `TradeControllerTest`, `PositionQueryServiceTest`) | 31 | 26 | 5 |
| **Total** | **123** | **100** | **23** |

All tests are hermetic: controllers run in a `@WebMvcTest` slice with `@MockBean` services,
outbound HTTP is served by `MockRestServiceServer` bound to a `RestTemplate` injected with
`ReflectionTestUtils`, and services are tested with plain Mockito repositories. No database,
docker-compose, socket.io feed or network access is required.

Also deleted (dead scaffolding in the wrong source set, never compiled or executed):
`account-service/src/main/test/java/finos/traderx/accountservice/AccountServiceApplicationTests.java`
and `position-service/src/main/test/java/finos/traderx/accountservice/AccountServiceApplicationTests.java`.

## Files added

- `trade-service/src/test/java/finos/traderx/tradeservice/controller/TradeOrderControllerTest.java`
- `account-service/src/test/java/finos/traderx/accountservice/controller/AccountControllerTest.java`
- `account-service/src/test/java/finos/traderx/accountservice/controller/AccountUserControllerTest.java`
- `account-service/src/test/java/finos/traderx/accountservice/service/AccountServiceTest.java`
- `position-service/src/test/java/finos/traderx/positionservice/controller/PositionControllerTest.java`
- `position-service/src/test/java/finos/traderx/positionservice/controller/TradeControllerTest.java`
- `position-service/src/test/java/finos/traderx/positionservice/service/PositionQueryServiceTest.java`

---

## (a) Test cases

Rows marked **DISABLED** encode the *expected* (correct) behaviour and are `@Disabled`
because the product does not behave that way today; see section (b).

### trade-service — `POST /trade/`

| ID | Area | Edge / corner case | Expected behaviour (as asserted) |
|---|---|---|---|
| TS-01a | happy path | valid Buy order | 200, exactly one `publish("/trades", order)`, outbound `GET {ref}/stocks/AAPL` then `GET {acct}/account/1` |
| TS-01b | happy path | valid Sell order | 200, exactly one publish |
| TS-02 | validation | reference-data 404 | 404, nothing published, account-service never called |
| TS-03 | validation | account-service 404 | 404, nothing published |
| TS-04a | upstream errors | reference-data 403 (4xx that is not 404) | 404 to the caller — every `HttpClientErrorException` is treated as "ticker unknown" |
| TS-04b | upstream errors | reference-data 500 | `HttpServerErrorException` escapes the controller unhandled; nothing published |
| TS-04c | upstream errors | reference-data 500 | **DISABLED** — should be a 502-style gateway response |
| TS-05 | upstream errors | reference-data connection refused | `ResourceAccessException` escapes unhandled |
| TS-05b | upstream errors | account-service connection refused | `ResourceAccessException` escapes unhandled |
| TS-06a | quantity | `0` | 200 and published with quantity 0 (no validation) |
| TS-06b | quantity | `0` | **DISABLED** — should be 400 |
| TS-06c | quantity | `-500` | 200 and published with quantity -500 |
| TS-06d | quantity | `-500` | **DISABLED** — should be 400 |
| TS-06e | quantity | `1` | 200 |
| TS-06f | quantity | `Integer.MAX_VALUE` | 200, value forwarded verbatim |
| TS-06g | quantity | `2147483648` (int overflow) | 400 from Jackson, no outbound calls, nothing published |
| TS-07a | null fields | `quantity: null` | 200, published with a null quantity |
| TS-07b | null fields | `security: null` | outbound URL is `{ref}/stocks/null`; 404 |
| TS-07c | null fields | `accountId: null` | outbound URL is `{acct}/account/null`; 404 |
| TS-07d | null fields | `side: null` | 200 and published with a null side |
| TS-07e | null fields | `side: null` | **DISABLED** — should be 400 |
| TS-08a | ticker | empty string | outbound URL `{ref}/stocks/`; 404 |
| TS-08b | ticker | whitespace only (`"   "`) | URI collapses to `{ref}/stocks` (the *collection* endpoint); the array response cannot be read as a `Security` and the `RestClientException` escapes unhandled |
| TS-08b2 | ticker | whitespace only | **DISABLED** — should be 400 |
| TS-08c | ticker | `../../account/1` | ticker is concatenated unescaped into the outbound path (`..` survives) |
| TS-08d | ticker | `../../account/1` | **DISABLED** — should stay inside `/stocks/` |
| TS-09a | ticker | 120 chars (DB column is 50) | 200 and published verbatim |
| TS-09b | ticker | 120 chars | **DISABLED** — should be rejected up-front |
| TS-09c | ticker | unicode `ÉÉÉ` | percent-encoded to `{ref}/stocks/%C3%89%C3%89%C3%89` |
| TS-10a | enum binding | `side: "BUY"` | 400 |
| TS-10b | enum binding | `side: "buy"` | 400 |
| TS-10c | enum binding | `side: "Short"` | 400 |
| TS-11a | payload | truncated JSON | 400, no outbound calls, nothing published |
| TS-11b | payload | empty body | 400 |
| TS-11c | payload | body is literal `null` | 400 |
| TS-12a | trust boundary | client-supplied `id` | accepted, forwarded to the feed and echoed back |
| TS-12b | trust boundary | client-supplied `state` | bound despite having no setter, forwarded to the feed as `Settled` |
| TS-12b2 | trust boundary | client-supplied `state` | **DISABLED** — should be server-owned/ignored |
| TS-12c | payload | unknown JSON properties | ignored, 200 |
| TS-13 | publisher | `PubSubException` on publish | wrapped in `RuntimeException`, escapes the controller (500 class failure), root cause preserved |
| TS-14a | protocol | `Content-Type: text/plain` | 415 |
| TS-14b | protocol | no `Content-Type` | 415 |
| TS-15 | routing | `POST /trade` (no trailing slash) | 404 — the mapping is `/trade` + `/` |

### account-service

| ID | Area | Edge / corner case | Expected behaviour (as asserted) |
|---|---|---|---|
| AS-16 | `GET /account/{id}` | missing id | 404, body is the raw exception message |
| AS-16b | `AccountService` | missing id | `ResourceNotFoundException("Account with id 42not found")` (note the missing space) |
| AS-16c | `AccountService` | id `0` and `-1` present in the repo | returned normally |
| AS-17a | `GET /account/{id}` | non-numeric id | 500 (not 400) |
| AS-17b | `GET /account/{id}` | non-numeric id | **DISABLED** — should be 400 |
| AS-17c | `GET /account/{id}` | non-numeric id | response body contains the internal conversion message |
| AS-17d | `GET /account/{id}` | id `-1` | 200, passed through to the service |
| AS-17e | `GET /account/{id}` | id `0` | 200 |
| AS-17f | `GET /account/{id}` | `2147483648` (> int) | 500 |
| AS-18a | `POST /account/` | `displayName` of 120 chars (column is 50) | 200, forwarded verbatim |
| AS-18b | `POST /account/` | `displayName` of 120 chars | **DISABLED** — should be 400 |
| AS-18c | `POST /account/` | empty `displayName` | 200 |
| AS-18d | `POST /account/` | null `displayName` | 200, null forwarded |
| AS-18e | `POST /account/` | unicode `displayName` | 200, round-trips |
| AS-18f | `POST /account/` | malformed JSON body | 500 (not 400) |
| AS-18g | `POST /account/` | malformed JSON body | **DISABLED** — should be 400 |
| AS-19 | `POST /account/` | client-supplied existing `id` | 200; the id reaches the service unchanged |
| AS-19b | `POST /account/` | client-supplied existing `id` | **DISABLED** — should be 409 |
| AS-19c | `AccountService` | `upsertAccount` with an explicit id | `save()` called, `findById` never called (no create/update distinction) |
| AS-19d | `AccountService` | `upsertAccount` over an existing row | **DISABLED** — should refuse to overwrite |
| AS-20 | `PUT /account/` | unknown id | 200 (silently creates) |
| AS-20b | `PUT /account/` | unknown id | **DISABLED** — should be 404 |
| AS-21 | `GET /account/` | empty repository | `[]`, not `null` |
| AS-21b | `GET /account/` | two rows | both serialised |
| AS-21c/d | `AccountService` | `getAllAccount` empty / populated | non-null empty list; all rows copied |
| AS-22a | `POST /accountuser/` | valid person | 200, outbound `GET {people}/People/GetPerson?LogonId=bob`, saved |
| AS-22b | `POST /accountuser/` | people-service 404 | 404, body `"nobody not found in People service."`, nothing saved |
| AS-22c | `POST /accountuser/` | people-service 500 | 500 via `@ExceptionHandler(Exception.class)`, nothing saved |
| AS-22d | `POST /accountuser/` | people-service unreachable | 500 whose body echoes the internal people-service URL |
| AS-22e | `POST /accountuser/` | people-service unreachable | **DISABLED** — should be 503 without internal detail |
| AS-23a | `POST /accountuser/` | username `bob&IsAdmin=true` | injected verbatim: `?LogonId=bob&IsAdmin=true` |
| AS-23b | `POST /accountuser/` | username `bob&IsAdmin=true` | **DISABLED** — should be percent-encoded |
| AS-23c | `POST /accountuser/` | username with a space | encoded as `%20` |
| AS-23d | `POST /accountuser/` | unicode username | encoded as `%C3%A9lodie` |
| AS-23e | `POST /accountuser/` | empty username | `?LogonId=` sent; 404 |
| AS-24a | `PUT /accountuser/` | any username | 200, **zero** outbound people-service calls |
| AS-24b | `PUT /accountuser/` | unknown username | **DISABLED** — should validate like POST |
| AS-24c | `GET /accountuser/{id}` | missing id | 404 with message body |
| AS-24d | `GET /accountuser/{id}` | non-numeric id | 500 |
| AS-24e | `GET /accountuser/` | empty repository | `[]` |
| AS-24f | `POST /accountuser/` | valid person, unknown account | 404 from the service is propagated |
| AS-24g | `POST /accountuser/` | `username: null` | `?LogonId=null` sent; 404 `"null not found in People service."` |
| AS-24h | `POST /accountuser/` | round trip | both composite key parts serialised |
| AS-24i | `AccountUserService` | unknown account id | `ResourceNotFoundException`, nothing saved |
| AS-24j | `AccountUserService` | existing account | saved and returned |
| AS-24k | `AccountUserService` | null accountId | throws, nothing saved |
| AS-27a | error handling | service throws `IllegalStateException` | 500 whose body is the raw message (JDBC URL leaked) |
| AS-27b | error handling | service throws | **DISABLED** — should not echo the internal message |

### position-service

| ID | Area | Edge / corner case | Expected behaviour (as asserted) |
|---|---|---|---|
| PS-25a | `GET /positions/{id}` | unknown account | 200 `[]` (no 404) |
| PS-25b | `GET /positions/{id}` | `-1` | 200 `[]` |
| PS-25c | `GET /positions/{id}` | `0` | 200 `[]` |
| PS-25d | `GET /positions/{id}` | non-numeric | 500, body contains the offending value |
| PS-25e | `GET /positions/{id}` | non-numeric | **DISABLED** — should be 400 |
| PS-25f | `GET /trades/{id}` | unknown account | 200 `[]` |
| PS-25g | `GET /trades/{id}` | `-1`, `0` | 200 `[]` |
| PS-25h | `GET /trades/{id}` | non-numeric | 500 |
| PS-25i | `GET /trades/{id}` | non-numeric | **DISABLED** — should be 400 |
| PS-25j | `PositionService` | ids `0` / `-1` | delegated to the repository unchanged |
| PS-25k | `PositionService` | repository throws | exception propagates to the controller |
| PS-25l | `PositionService` | repository returns `null` | `null` is passed straight through |
| PS-25m | `PositionService` | repository returns `null` | **DISABLED** — should never return null |
| PS-26a | `GET /positions/` | empty repository | `[]`, not `null` |
| PS-26b | routing | `GET /positions` (no trailing slash) | 404 |
| PS-26c | `GET /trades/` | empty repository | `[]` |
| PS-26d | routing | `GET /trades` (no trailing slash) | 404 |
| PS-26e/f/g | services | `getAllPositions` / `getAllTrades` | non-null empty lists; all rows copied |
| PS-27a | error handling | repository throws | 500 whose body is exactly the raw exception message (SQL leaked) |
| PS-27b | error handling | repository throws | **DISABLED** — should not leak SQL |
| PS-27c | error handling | `/trades/` repository throws | 500 with the raw JDBC message |
| PS-27d | error handling | `/trades/` repository throws | **DISABLED** — should not leak SQL |
| PS-28a | data | position with quantity `0` | returned with `"quantity":0` |
| PS-28b | data | short position `-250` | returned with the sign intact |
| PS-28c | data | position with a null quantity | serialised as `"quantity":null`, 200 |
| PS-28d | data | trades with `0` and `-100` quantities | returned unchanged |
| PS-28e | data | trade with no explicit state | `"state":"UNSET"` (the field default) |
| PS-28f | data | 9-char `side` in a 4-char column | serialised as-is on the read path |
| PS-28g | `PositionService` | zero and negative quantities | survive the service layer untouched |

---

## (b) Latent bugs surfaced

Nothing below was fixed; each has one `@Disabled` test that encodes the correct behaviour,
plus an enabled test that pins the current behaviour so a future fix will visibly flip them.

### 1. Reference-data 5xx is not handled — `TS-04c`
- **File**: `trade-service/.../controller/TradeOrderController.java:80` (and `:105` for account-service)
- **Today**: `validateTicker`/`validateAccount` catch only `HttpClientErrorException`. A 5xx or a
  connection failure raises `HttpServerErrorException` / `ResourceAccessException`, which nothing
  catches — the request dies with an unhandled exception and there is no `@ExceptionHandler` in this
  controller at all.
- **Should**: distinguish "ticker unknown" (404) from "upstream unavailable" and return 502/503.
- **Severity**: Medium (availability + confusing client contract).

### 2. Any 4xx from reference-data/account-service is reported as "not found" — `TS-04a` (enabled, documents it)
- **File**: `TradeOrderController.java:80-88`, `:105-113`
- **Today**: `catch (HttpClientErrorException)` returns `false` for 401/403/429 as well as 404, so an
  auth or rate-limit failure is reported to the trader as "AAPL not found in Reference data service."
- **Should**: only 404 means "unknown"; other 4xx should surface as an upstream error.
- **Severity**: Medium.

### 3. Path traversal / SSRF in the reference-data URL — `TS-08d`
- **File**: `TradeOrderController.java:72` — `referenceDataServiceAddress + "//stocks/" + ticker`
- **Today**: the ticker is neither validated nor encoded as a single path segment; a body with
  `"security": "../../account/1"` produces an outbound request whose path escapes `/stocks/`
  (verified in `TS-08c`).
- **Should**: use `UriComponentsBuilder`/`RestTemplate` URI variables so the ticker is always one
  encoded path segment, and reject tickers that do not match a ticker charset.
- **Severity**: High (attacker-controlled outbound URL against an internal service).

### 4. Whitespace-only ticker hits the reference-data collection endpoint — `TS-08b2`
- **File**: `TradeOrderController.java:72`
- **Today**: `"   "` is trimmed while the URI is built, so the call becomes `GET {ref}/stocks`
  (the list endpoint). Its array body cannot be deserialised into `Security`, so the request fails
  with an unhandled `RestClientException` instead of a clean validation error.
- **Should**: reject blank tickers with 400 before any outbound call.
- **Severity**: Medium.

### 5. No quantity validation — `TS-06b`, `TS-06d`
- **File**: `TradeOrderController.java:44-66` (no validation anywhere in `createTradeOrder`)
- **Today**: `quantity` of `0`, negative values and `null` are accepted and published to `/trades`.
  A negative Buy inverts the sign of the resulting position in trade-processor.
- **Should**: require `quantity >= 1` (400 otherwise).
- **Severity**: High (bad economic data reaches the book).

### 6. No `side` validation for `null` — `TS-07e`
- **File**: `TradeOrderController.java:44-66`
- **Today**: `"side": null` is accepted and published; only a *wrong* enum literal is rejected (by
  Jackson, not by the controller).
- **Should**: 400 when `side` is absent or null.
- **Severity**: Medium.

### 7. `security` length is never checked against the 50-char column — `TS-09b`
- **File**: `TradeOrderController.java:44-66`; column defined in
  `position-service/.../model/Position.java` (`@Column(length = 50, name = "SECURITY")`)
- **Today**: a 120-char ticker is accepted and published; it only fails later, in the consumer, when
  the row is persisted — an asynchronous failure the trader never sees.
- **Should**: validate length at the API boundary.
- **Severity**: Low/Medium.

### 8. Client can set the order `state` — `TS-12b2`
- **File**: `trade-service/.../model/TradeOrder.java:6` (`private String state;`, getter only)
- **Today**: Jackson still binds `state` from the request body (verified in `TS-12b`), so a caller can
  submit an order that already claims to be `Settled` and it is published on the feed that way.
  The `id` field (`TradeOrder.java:5`, `public String id`) is likewise caller-controlled (`TS-12a`).
- **Should**: `state` (and ideally `id`) must be server-owned — annotate `@JsonIgnore`/`@JsonProperty(access = READ_ONLY)`.
- **Severity**: Medium (trust-boundary violation on lifecycle state).

### 9. `POST /account/` with an existing id silently overwrites — `AS-19b`, `AS-19d`
- **File**: `account-service/.../service/AccountService.java:34` (`return this.accountRepository.save(account);`),
  reached from `AccountController.java:37-40`
- **Today**: the caller supplies the primary key and `save()` overwrites the existing row. There is no
  existence check, no ownership check and no authentication anywhere in the service.
- **Should**: `POST` should create only (409 on an existing id) and the id should be server-generated.
- **Severity**: High (any caller can rename/replace another trader's account).

### 10. `PUT /account/` creates instead of 404 — `AS-20b`
- **File**: `AccountController.java:42-45` → `AccountService.upsertAccount` (`:34`)
- **Today**: updating an id that does not exist silently inserts a new row.
- **Should**: 404 when the target account does not exist.
- **Severity**: Medium.

### 11. `PUT /accountuser/` skips person validation that `POST` performs — `AS-24b`
- **File**: `account-service/.../controller/AccountUserController.java:59-62` vs `:49-57`
- **Today**: `POST` calls `validatePerson()` against people-service; `PUT` does not call it at all
  (verified in `AS-24a`: zero outbound calls), so any username string can be linked to an account.
- **Should**: both write paths must validate identically.
- **Severity**: High (identity binding bypass).

### 12. Query-parameter injection into the people-service URL — `AS-23b`
- **File**: `AccountUserController.java:70` — `peopleServiceAddress + "/People/GetPerson" + "?LogonId=" + username`
- **Today**: the username is concatenated raw; `bob&IsAdmin=true` produces
  `?LogonId=bob&IsAdmin=true` on the outbound call (verified in `AS-23a`). Spaces and unicode *are*
  encoded, but reserved query characters (`&`, `=`) are not.
- **Should**: build the URI with a query parameter placeholder so reserved characters are encoded.
- **Severity**: Medium/High (parameter smuggling into the identity service).

### 13. Malformed input is reported as 500 — `AS-17b`, `AS-18g`, `PS-25e`, `PS-25i`
- **File**: `AccountController.java:59`, `AccountUserController.java:95`,
  `PositionController.java:40`, `TradeController.java:39` — `@ExceptionHandler(Exception.class)`
- **Today**: the catch-all also swallows `MethodArgumentTypeMismatchException` and
  `HttpMessageNotReadableException`, so a bad path variable or malformed JSON returns 500 instead of 400.
- **Should**: let Spring's default handling produce 400 for client errors; keep the catch-all for
  genuine server faults only.
- **Severity**: Low (monitoring noise, misleading client contract).

### 14. Internal exception messages are echoed to the caller — `AS-27b`, `PS-27b`, `PS-27d`, `AS-22e`
- **File**: `AccountController.java:59-61`, `AccountUserController.java:95-97`,
  `PositionController.java:40-42`, `TradeController.java:39-41` — all return `e.getMessage()` as the body
- **Today**: the body of a 500 contains SQL text, JDBC URLs (`jdbc:h2:tcp://db:18082/traderx`) and
  internal service URLs, on endpoints with no authentication.
- **Should**: return a generic message plus a correlation id; log the detail server-side.
- **Severity**: Medium (information disclosure).

### 15. `PositionService.getPositionsByAccountID` can return `null` — `PS-25m`
- **File**: `position-service/.../service/PositionService.java:26` (and `TradeService.java:26`)
- **Today**: the repository result is returned verbatim with no null guard, unlike `getAllPositions`
  which always builds a list; a null would serialise as a null JSON body rather than `[]`.
- **Should**: return an empty list.
- **Severity**: Low (defensive; Spring Data does not currently return null for a `List` query).

### 16. Cosmetic: malformed "not found" messages
- **File**: `AccountService.java:29` (`"Account with id " + id + "not found"`),
  `AccountUserService.java:35` and `:42`
- **Today**: the message renders as `Account with id 42not found` (missing space). Pinned by
  `AS-16`, `AS-16b`, `AS-24i` so the text is not changed accidentally.
- **Should**: `"Account with id 42 not found"`.
- **Severity**: Cosmetic.

### Cases noted rather than tested

- **`POST /trade/` with a 200 but empty body from reference-data** would NPE at
  `TradeOrderController.java:78` (`response.getBody().toString()`). Not asserted as a separate case
  because it is the same unhandled-exception path already covered by TS-04b/TS-05.
- **Authentication/authorization** cannot be tested: none of the three services define any security
  configuration, so every endpoint (including the overwrite paths in bugs 9-11) is anonymous.
