# TraderX Edge & Corner Case Audit

Audit of the trade-service, trade-processor, position-service and account-service REST APIs plus the
Angular web front end, focused on behaviour that is **not currently exercised by any test**.

## Baseline: coverage before this change

| Area | Test files present | Tests actually executed |
|---|---|---|
| trade-service | 0 | 0 |
| account-service | 1 empty stub in `src/main/test/java` (wrong source set) | 0 — Gradle reports `test NO-SOURCE` |
| position-service | 1 empty stub in `src/main/test/java` (wrong source set) | 0 — `test NO-SOURCE` |
| trade-processor | 1 empty stub in `src/main/test/java` (wrong source set) | 0 — `test NO-SOURCE` |
| Angular front end | 9 `*.spec.ts` files | 0 — every suite is disabled with `xdescribe(...)` |

So the platform shipped with **zero effective automated test coverage**. Java tests were in
`src/main/test/java/...` instead of `src/test/java/...`, which Gradle silently ignores.

## Enumerated edge & corner cases

IDs are referenced by the added tests and by the per-area reports
(`TEST_REPORT_api.md`, `TEST_REPORT_ui.md`, `TEST_REPORT_lifecycle.md`).

### A. Boundary values

| ID | Case | Where |
|---|---|---|
| A1 | `quantity = 0` order | trade-service `POST /trade/`, trade ticket UI |
| A2 | Negative `quantity` (sign is applied on top of the raw value, so a negative Buy behaves like a Sell) | trade-service, trade-processor |
| A3 | `quantity = Integer.MAX_VALUE`, and JSON `2147483648` (beyond `int`) | trade-service, trade-processor |
| A4 | Position quantity overflow: `position.quantity + newQuantity` on a 32-bit `int` | trade-processor `TradeService` |
| A5 | `security` longer than the 50-char `SECURITY` column; empty and whitespace-only ticker | all services |
| A6 | `displayName` longer than the 50-char column; empty; unicode | account-service |
| A7 | Zero and negative (short) position quantities returned by the read APIs and rendered by the blotters | position-service, UI |
| A8 | Empty result sets (`GET /positions/`, `GET /trades/`, `GET /account/`) | position-service, account-service |

### B. Trade lifecycle / invalid state transitions

| ID | Case | Where |
|---|---|---|
| B1 | `TradeState.Processing` is set and immediately overwritten by `Settled` in the same method, so no row is ever persisted or published in `Processing` — the intermediate state is unobservable | trade-processor `TradeService.processTrade` |
| B2 | No transition guard exists anywhere: `Settled -> New`, `Cancelled -> Settled` etc. are all accepted by the entity | trade-processor |
| B3 | `TradeState.Cancelled` is declared but never produced or consumed — there is no cancel path in any service | model-wide |
| B4 | Client-supplied `id` and `state` on the inbound `TradeOrder` are ignored/overwritten downstream (trust boundary is undocumented) | trade-service, trade-processor |
| B5 | Enum column widths (`STATE` 20, `SIDE` 4) leave no headroom for new states/sides | trade-processor entities |

### C. Concurrency & race windows

| ID | Case | Where |
|---|---|---|
| C1 | Lost update: `findByAccountIdAndSecurity` → mutate → `save` is a read-modify-write with no locking, no `@Transactional`, and no optimistic-locking `@Version`. Two concurrent trades on the same (account, security) can drop one update | trade-processor `TradeService.processTrade` |
| C2 | Non-atomic double write: `tradeRepository.save` and `positionRepository.save` are separate, unwrapped transactions — a failure between them leaves a booked trade with a stale position | trade-processor |
| C3 | Duplicate delivery: the trade id is generated server-side (`UUID.randomUUID()`) and the order's own id is ignored, so an at-least-once redelivery from the feed books the trade twice — no idempotency key | trade-processor `TradeFeedHandler` |
| C4 | Concurrent trades on the same account but different securities must not cross-talk | trade-processor |
| C5 | UI: out-of-order / duplicate socket updates for the same trade id, and blotter updates arriving for an account other than the selected one | Angular blotters |
| C6 | UI: switching account while a trade-feed subscription is live (subscription teardown / cross-talk) | Angular `trade-feed.service` |
| C7 | Rapid double-click on the trade ticket create button submits two identical orders | Angular trade ticket |

### D. Error-path handling

| ID | Case | Where |
|---|---|---|
| D1 | Reference-data / account-service returns **5xx**: `validateTicker`/`validateAccount` only catch `HttpClientErrorException`, so a 4xx of any kind is reported to the user as "not found", while a 5xx (`HttpServerErrorException`) escapes uncaught and becomes a 500 | trade-service `TradeOrderController` |
| D2 | Downstream service unreachable (`ResourceAccessException`) — same gap as D1 | trade-service, account-service |
| D3 | `PubSubException` on publish is wrapped in a bare `RuntimeException` → opaque 500 to the caller | trade-service |
| D4 | `processTrade` swallows publish failures entirely: the trade and position are persisted but no event reaches the UI, and the caller still sees success | trade-processor |
| D5 | `TradeFeedHandler.onMessage` catches every exception and only logs — a poison message is dropped with no retry and no dead-letter queue | trade-processor |
| D6 | `@ExceptionHandler(Exception.class)` returns the raw exception message in the response body (internal detail disclosure) | position-service, account-service |
| D7 | Malformed JSON, empty body, wrong `Content-Type`, invalid enum value for `side` | all POST endpoints |
| D8 | UI: HTTP 404/500/timeout from the account/position/trade services must not leave the component in a broken state | Angular services |
| D9 | UI: malformed or partial socket payloads (missing `quantity`/`state`/`id`, null) must not crash the blotter | Angular blotters |

### E. Auth / ID validation gaps

| ID | Case | Where |
|---|---|---|
| E1 | No authentication or authorization on **any** endpoint; `@CrossOrigin("*")` is set on every controller | all services |
| E2 | `POST /tradeservice/order` on trade-processor bypasses trade-service entirely, so ticker and account validation can be skipped by calling the processor directly | trade-processor |
| E3 | `POST /account/` with an explicit existing `id` silently overwrites that account (`save` is an upsert, no ownership check); `PUT /account/` with an unknown id creates one instead of returning 404 | account-service |
| E4 | `PUT /accountuser/` does not validate the person against people-service while `POST /accountuser/` does — asymmetric validation | account-service |
| E5 | Outbound URLs are built by raw string concatenation (`.../stocks/" + ticker`, `?LogonId=" + username`) — path traversal / query-parameter injection in the ticker and username values | trade-service, account-service |
| E6 | Non-numeric, negative and out-of-`int`-range path IDs (`/account/{id}`, `/positions/{accountId}`, `/trades/{accountId}`) | account-service, position-service |
| E7 | Unknown account on the position/trade read APIs returns `200 []` rather than `404`, so the caller cannot distinguish "no positions" from "no such account" | position-service |
| E8 | UI: `accountId` falls back to `0` when no account is selected, letting a trade be submitted against a non-existent account 0 | Angular trade ticket |
| E9 | UI: display names / tickers are rendered without an explicit escaping test (XSS regression guard) | Angular |

### F. Input validation gaps (front end)

| ID | Case | Where |
|---|---|---|
| F1 | `onCreate()` rejects quantity 0 via a falsy check but shows no user-visible error — the click appears to do nothing | trade ticket |
| F2 | Fractional and string quantities are not rejected client-side | trade ticket |
| F3 | A free-typed ticker that is not in the typeahead list is only cleared on blur when `selectedCompany` is falsy | trade ticket |
| F4 | No client-side upper bound on quantity, so values beyond Java `int` reach the server | trade ticket |

## Coverage after this change

| Suite | How to run | Tests | Passing | Disabled (bug-exposing) |
|---|---|---|---|---|
| trade-service | `./gradlew :trade-service:test` | 43 | 35 | 8 |
| account-service | `./gradlew :account-service:test` | 49 | 39 | 10 |
| position-service | `./gradlew :position-service:test` | 31 | 26 | 5 |
| trade-processor | `./gradlew :trade-processor:test` | 44 | 42 | 2 |
| Angular (Karma/Jasmine) | `npm run test:ci` in `web-front-end/angular` | 101 | 94 | 7 |
| Playwright e2e | `npx playwright test` (needs the full stack up; not wired into CI) | 12 tests in 3 specs | n/a | n/a |

Which enumerated case is covered where:

| Group | Covered by |
|---|---|
| A. Boundary values | `TEST_REPORT_api.md` TS-06*, TS-08*, TS-09*, AS-18*, PS-25*; `TEST_REPORT_lifecycle.md` QT-01..QT-09; `TEST_REPORT_ui.md` UI-TT-06..UI-TT-11 |
| B. Lifecycle / invalid transitions | `TEST_REPORT_lifecycle.md` LC-01..LC-06, TS-12b2 |
| C. Concurrency & races | `TEST_REPORT_lifecycle.md` CC-01..CC-05, FD-01..FD-03; `TEST_REPORT_ui.md` UI-TB-07/09/11, UI-PB-06, UI-TT-22 |
| D. Error paths | `TEST_REPORT_api.md` TS-04*, AS-22e, AS-27b, PS-27*; `TEST_REPORT_ui.md` UI-TB-15/23, UI-PB-09 |
| E. Auth / ID validation | `TEST_REPORT_api.md` AS-17b, AS-19*, AS-20b, AS-23b, AS-24b, PS-25e/i; `TEST_REPORT_lifecycle.md` API-05, API-07 |
| F. Front-end input validation | `TEST_REPORT_ui.md` UI-TT-06..UI-TT-11, UI-TT-22 |

Not covered by an automated test (documented only): E1 (no auth anywhere — architectural), E9 (XSS escaping
is handled by Angular's default sanitiser; no regression guard added), A5/A6 column-width truncation at the
DB layer, and B5 enum column headroom beyond the `SIDE` case (`LC-06`).

## Latent bugs surfaced

None of these were fixed. Each is either encoded by a `@Disabled`/`xit` test asserting the *correct*
behaviour, or asserted as current (buggy) behaviour by an enabled test that will fail once fixed.

| # | Bug | Location | Severity | Evidence |
|---|---|---|---|---|
| 1 | Position arithmetic overflows a 32-bit `int` — a large buy on a long position flips negative | `trade-processor` `TradeService.processTrade` | High | disabled `QT-07` |
| 2 | Lost update: unsynchronised read-modify-write of `Position`, no `@Transactional`/`@Version`/lock | `trade-processor` `TradeService.processTrade` | High | disabled `CC-01`, companion `CC-02` |
| 3 | Trade and position writes are not in one transaction — a failure between them leaves a stale position | `trade-processor` `TradeService` | High | `CC-04` |
| 4 | Publish failures are swallowed; caller sees success but the UI never gets the event | `trade-processor` `TradeService` | Medium | `CC-05` |
| 5 | Feed handler drops poison messages, no retry and no DLQ | `trade-processor` `TradeFeedHandler` | High | `FD-01`, `FD-02` |
| 6 | No idempotency key — at-least-once redelivery double-books the trade | `trade-processor` `TradeFeedHandler` | High | `FD-03` |
| 7 | No quantity validation: `0` books a no-op trade, negative inverts the side, `null` throws NPE | trade-service + trade-processor | High | `TS-06b`, `TS-06d` (disabled), `QT-02/03/05/06/09` |
| 8 | `POST /tradeservice/order` is unauthenticated and bypasses ticker/account validation | `trade-processor` `TradeServiceController` | High | `API-05`, `API-07` |
| 9 | No state-transition guard; `Cancelled` is a dead state; `Processing` is never observable | trade-processor model | Medium | `LC-01`, `LC-03..LC-05` |
| 10 | Reference-data/account-service 5xx escapes as an opaque 500; every 4xx is reported as "not found" | `trade-service` `TradeOrderController` | Medium | disabled `TS-04c`, `TS-04a` |
| 11 | Path traversal / SSRF via raw ticker concatenation into the reference-data URL | `trade-service` `TradeOrderController` | High | disabled `TS-08d`, `TS-08b2` |
| 12 | Query-parameter injection via raw username concatenation into the people-service URL | `account-service` `AccountUserController` | High | disabled `AS-23b` |
| 13 | `POST /account/` with an existing id silently overwrites it; no ownership check | `account-service` `AccountService.upsertAccount` | High | disabled `AS-19b`, `AS-19d` |
| 14 | `PUT /account/` with an unknown id creates instead of returning 404 | `account-service` | Medium | disabled `AS-20b` |
| 15 | `PUT /accountuser/` skips the people-service validation that `POST` performs | `account-service` `AccountUserController` | Medium | disabled `AS-24b` |
| 16 | Malformed/non-numeric input is reported as 500 rather than 400 | account-service, position-service | Medium | disabled `AS-17b`, `AS-18g`, `PS-25e`, `PS-25i` |
| 17 | Internal exception messages (incl. SQL) are echoed to the caller | position-service, account-service | Medium | disabled `AS-27b`, `PS-27b`, `PS-27d`, `AS-22e` |
| 18 | `PositionService.getPositionsByAccountID` can return `null` | `position-service` | Low | disabled `PS-25m` |
| 19 | Client-supplied `state` on a `TradeOrder` is accepted at the trust boundary | trade-service | Low | disabled `TS-12b2` |
| 20 | `security` length is never checked against the 50-char column | trade-service | Low | disabled `TS-09b` |
| 21 | `PositionID` overrides neither `equals` nor `hashCode`; `TradeRepository` declares the wrong id type | trade-processor persistence | Low | `DB-10`, `DB-11` |
| 22 | Trade blotter accepts feed messages belonging to a different account | Angular trade blotter | High | disabled `UI-TB-07` |
| 23 | Row-id prefix mismatch means trade and position updates never match an existing row (duplicates instead of in-place update) | Angular trade + position blotters | High | disabled `UI-TB-09`, `UI-PB-06` |
| 24 | Out-of-order updates leave a stale state visible | Angular trade blotter | Medium | disabled `UI-TB-11` |
| 25 | `null` feed payloads throw instead of being ignored | Angular trade + position blotters | Medium | disabled `UI-TB-15`, `UI-PB-09` |
| 26 | Blotter never leaves the pending state when the trades request fails or hangs | Angular trade blotter | Medium | disabled `UI-TB-23` |
| 27 | User search never re-queries when the search text changes | Angular `assign-user.component` | Medium | see `TEST_REPORT_ui.md` §8 |
| 28 | Trade ticket rejects invalid input (incl. quantity `0`) with no user feedback, has no client-side quantity validation and no duplicate-submission guard | Angular trade ticket | Medium | `UI-TT-06..11`, `UI-TT-22` |

## One production change was required

`main/app/accounts/user/assign-user.component.ts` did not typecheck (`switchMap<string, ...>` fed by an
`Observable<string | undefined>`), which failed the whole Angular build before any spec could run. This is
pre-existing on `main` — it was invisible only because every suite was disabled. The generic was widened to
`string | undefined`; no runtime behaviour changed.

