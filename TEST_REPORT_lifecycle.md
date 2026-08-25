# TEST REPORT — Trade lifecycle, matching & concurrency (trade-processor)

Branch: `devin/swarm-lifecycle-tests`
Command: `./gradlew :trade-processor:test --console=plain`
Result: **44 tests, 42 passing, 2 disabled because they expose latent product bugs.**

Files added:

| File | Contents |
| --- | --- |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/service/TradeServiceTestSupport.java` | Test helpers (order/position builders, save recorders) |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/service/TradeServiceStateMachineTest.java` | LC-01..LC-06 |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/service/TradeServiceQuantityTest.java` | QT-01..QT-12 |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/service/TradeServiceConcurrencyTest.java` | CC-01..CC-05 |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/TradeFeedHandlerTest.java` | FD-01..FD-03 |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/controller/TradeServiceControllerTest.java` | API-01..API-07 |
| `trade-processor/src/test/java/finos/traderx/tradeprocessor/repository/TradePositionPersistenceTest.java` | DB-01..DB-11 (`@DataJpaTest`, in-memory H2) |
| `trade-processor/src/test/resources/application.properties` | Hermetic in-memory datasource for tests |

Deleted: `trade-processor/src/main/test/java/finos/traderx/accountservice/AccountServiceApplicationTests.java`
(dead stub in the wrong source set — it was never compiled or executed).

All tests are hermetic: Mockito for repositories and publishers, `@WebMvcTest` + MockMvc for the
controller, `@DataJpaTest` with an embedded H2 for entity/repository behaviour. No docker-compose,
no H2 TCP server, no socket.io, no other service.

## (a) Test cases

| ID | Area | Edge / corner case exercised | Expected behaviour asserted |
| --- | --- | --- | --- |
| LC-01 | State machine | `processTrade` saves twice, mutating state between saves | Only `New` then `Settled` are ever handed to `save`; `Processing` is never persisted; returned trade is `Settled` |
| LC-02 | State machine | Ordering of publish vs. writes | The trade published to `/accounts/{id}/trades` is the `Settled` instance |
| LC-03 | State machine | Illegal direct transition `New -> Settled` on the entity | Accepted silently — no transition guard exists |
| LC-04 | State machine | Backwards transitions `Settled -> New` and `Cancelled -> Settled` | Both accepted silently — no transition guard exists |
| LC-05 | State machine | `TradeState.Cancelled` reachability | No processing path produces `Cancelled`; `TradeService` exposes no cancel operation (dead state) |
| LC-06 | Schema | `STATE` is `length=20`, `SIDE` is `length=4` | Every enum constant fits; `Sell` uses the SIDE column exactly, i.e. zero headroom |
| QT-01 | Arithmetic | Buy 100 then Sell 100 | Position row survives with quantity 0 (row is not deleted) |
| QT-02 | Arithmetic | Sell with no existing position | Creates a short position of −50, no validation |
| QT-03 | Arithmetic | Sell 40 against a held 10 | Oversells to −30, no available-quantity check |
| QT-04 | Arithmetic | `quantity = 0` order | Trade booked; position unchanged; both writes still happen |
| QT-05 | Arithmetic | Buy with `quantity = -30` | Position *decreases* to 70 while the trade is still recorded as a `Buy` (blotter and position disagree) |
| QT-06 | Arithmetic | Sell with `quantity = -30` | Position *increases* to 130 |
| QT-07 | Arithmetic | Buy `Integer.MAX_VALUE` on top of a long position | **Should** stay positive — **DISABLED, real overflow bug** (see B1) |
| QT-08 | Arithmetic | Same input as QT-07 | Documents the observed behaviour: wraps to a negative quantity |
| QT-09 | Null handling | `quantity = null` | `NullPointerException` on unboxing; **nothing** is persisted and nothing is published (the NPE happens before the first `save`) |
| QT-10 | Null handling | `security = null` | Booked; position created with a null security key (no ticker validation in the processor) |
| QT-11 | Null handling | `security = ""` and a 200-character security | Both accepted by the service layer; only the DB rejects the over-long one |
| QT-12 | Null handling | `accountId = null` | Booked; published to the literal topic `/accounts/null/trades` |
| CC-01 | Concurrency | Two concurrent buys on the same (account, security) | **Should** total 130 — **DISABLED, real lost-update bug** (see B2) |
| CC-02 | Concurrency | Same interleaving as CC-01 | Documents the observed lost update: final quantity is 100 or 30, never 130 |
| CC-03 | Concurrency | Two concurrent trades, same account, different securities | Both positions persist independently, no cross-talk |
| CC-04 | Atomicity | `positionRepository.save` throws after the trade was saved | The exception propagates, the `New` trade row is already written and never rolled back or settled; nothing is published; `TradeService` carries no `@Transactional` |
| CC-05 | Silent failure | `tradePublisher.publish` throws `PubSubException` | Swallowed: `processTrade` returns a successful `TradeBookingResult`, the position is never published, the UI never learns of the trade |
| FD-01 | Feed | Poison message (`processTrade` throws) | `onMessage` does not rethrow; the message is acknowledged and dropped; the handler has no retry/DLQ collaborator |
| FD-02 | Feed | Incomplete order producing an NPE | Also swallowed |
| FD-03 | Feed | Duplicate (at-least-once) delivery of the same `TradeOrder` | Booked twice with two different server-generated UUIDs; the order's own `id` is never used for de-duplication |
| API-01 | API | Happy path `POST /tradeservice/order` | 200 with a `TradeBookingResult` (trade + position) |
| API-02 | API | Malformed JSON | 400, service not invoked |
| API-03 | API | Empty body | 400, service not invoked |
| API-04 | API | Unknown `side` value (`"Short"`) | 400, service not invoked |
| API-05 | API | `{}` — every field missing | 200; the service is invoked with an all-null order (no request validation) |
| API-06 | API | Unknown extra field in the body | Ignored, 200 |
| API-07 | API | Non-existent account (999999) and ticker (`NOTATICKER`) | 200; no security annotations on the controller; `CrossOrigin("*")`; `TradeService` holds no HTTP client, i.e. reference-data/account validation is bypassed entirely |
| DB-01 | Persistence | All `TradeState` × `TradeSide` combinations | Round-trip through the `STATE`/`SIDE` columns, including `Cancelled` |
| DB-02 | Persistence | Two saves with the same (accountId, security) | Upsert: a single row with the latest quantity |
| DB-03 | Persistence | Same security under two accounts | Two independent rows |
| DB-04 | Persistence | Negative position | Persisted without complaint |
| DB-05 | Persistence | Unknown key lookup | `findByAccountIdAndSecurity` returns `null` (not `Optional`), `findByAccountId` returns empty |
| DB-06 | Persistence | 200-character security | Rejected by the database (`SECURITY` is `varchar(50)`) |
| DB-07 | Persistence | `null` security key component | Rejected — cannot be persisted |
| DB-08 | Persistence | `null` accountId key component | Rejected — cannot be persisted |
| DB-09 | Persistence | Empty-string security | A perfectly valid composite key |
| DB-10 | Persistence | `PositionID` as an `@IdClass` | Declares neither `equals` nor `hashCode`; two identical keys compare unequal |
| DB-11 | Persistence | `TradeRepository extends JpaRepository<Trade, Integer>` | The declared id type is `Integer` while `Trade.id` is a `String`; `findById(1)` can never match a row |

## (b) Latent bugs surfaced

### B1 — Integer overflow in position arithmetic (**disabled test: `QT-07 hugeBuyMustNotOverflow`**)
* Product file: `trade-processor/src/main/java/finos/traderx/tradeprocessor/service/TradeService.java`, lines 59–60.
* Today: `int newQuantity = ((side==Buy)?1:-1) * t.getQuantity();` followed by
  `position.setQuantity(position.getQuantity() + newQuantity);` — plain `int` arithmetic. A position of
  10 plus a buy of `Integer.MAX_VALUE` silently wraps to −2147483639, turning a huge long into a huge short.
* Should: use a widening/checked accumulation (`long`, `BigDecimal`, or `Math.addExact` with an explicit
  rejection) and reject orders that would overflow the position.
* Severity: **High** (silent data corruption of a book position; no exception, no log).

### B2 — Lost update: unsynchronised read-modify-write of `Position` (**disabled test: `CC-01 concurrentTradesMustNotLoseAnUpdate`**)
* Product file: `TradeService.java`, lines 50–63 (`findByAccountIdAndSecurity` → mutate → `save`).
* Today: there is no `@Transactional`, no pessimistic/optimistic lock and no `@Version` on `Position`.
  Two threads that read the same position before either writes both compute their delta from the same
  stale quantity, and the second `save` overwrites the first. Booked trades therefore disappear from
  the position. Virtual threads are enabled (`spring.threads.virtual.enabled=true`) and the feed
  handler and REST endpoint can both drive `processTrade` at once, so this is reachable in production.
* Should: apply the delta atomically — e.g. `@Transactional` plus `@Lock(PESSIMISTIC_WRITE)` on the
  position lookup, an `@Version` optimistic-lock column with retry, or a `UPDATE ... SET quantity = quantity + :delta` statement.
* Severity: **High** (money-losing: positions silently diverge from the trade blotter).

### B3 — No transaction spans the trade and position writes (test `CC-04`, enabled)
* Product file: `TradeService.java`, lines 62–71.
* Today: `tradeRepository.save(t)` (state `New`) is followed by `positionRepository.save(position)` and
  then a second trade save. If the position save fails, the `New` trade row is already committed and is
  never rolled back nor advanced to `Settled` — an orphan trade with no position update, permanently stuck in `New`.
* Should: `processTrade` should be `@Transactional` so both writes commit or roll back together.
* Severity: **High**.

### B4 — Publisher failures are swallowed (test `CC-05`, enabled)
* Product file: `TradeService.java`, lines 76–82.
* Today: `catch (PubSubException exc) { log.error(...) }` — the caller receives a successful
  `TradeBookingResult` even though neither the trade nor the position event reached the feed, so the
  blotters silently go stale. The position event is additionally skipped whenever the trade publish fails first.
* Should: publish outside the write path with a durable outbox/retry, or surface the failure to the caller.
* Severity: **Medium**.

### B5 — The feed handler drops poison messages with no retry or DLQ (test `FD-01`/`FD-02`, enabled)
* Product file: `trade-processor/src/main/java/finos/traderx/tradeprocessor/TradeFeedHandler.java`, lines 21–28.
* Today: `catch (Exception x) { log.error(...) }`. Any failing order is acknowledged and lost; there is no
  redelivery, no dead-letter destination and no metric.
* Should: negatively acknowledge / retry with backoff and route repeated failures to a dead-letter topic.
* Severity: **High** (silent trade loss).

### B6 — No idempotency: duplicate delivery double-books (test `FD-03`, enabled)
* Product files: `TradeFeedHandler.java` line 23 and `TradeService.java` line 41 (`t.setId(UUID.randomUUID().toString())`).
* Today: the socket.io feed is at-least-once, but the incoming `TradeOrder.id` is ignored and a fresh UUID
  is generated per delivery, so a redelivered order is booked as a second, indistinguishable trade and the
  position is moved twice.
* Should: key the trade on the order id (or keep a processed-order table) and make `processTrade` idempotent.
* Severity: **High**.

### B7 — No business validation of quantity or side effects on the position (tests `QT-02`, `QT-03`, `QT-05`, `QT-06`, enabled)
* Product file: `TradeService.java`, lines 46–60.
* Today: quantity is never range-checked. A sell with no holding creates a short; a sell larger than the
  holding oversells; a *negative* quantity inverts the meaning of the side, so a `Buy` reduces the position
  while the blotter still shows a buy. Zero-quantity trades are booked as real trades.
* Should: reject non-positive quantities, and either reject or explicitly authorise short positions.
* Severity: **Medium/High**.

### B8 — `null` quantity causes an unboxing `NullPointerException` (test `QT-09`, enabled)
* Product file: `TradeService.java`, line 59 (`... * t.getQuantity()` unboxes `Integer` to `int`).
* Today: an order with no quantity throws an NPE. Good news, verified by the test: the NPE happens *before*
  `tradeRepository.save`, so no partial write occurs. Via the REST endpoint this surfaces as an opaque 500;
  via the feed it is swallowed (B5).
* Should: validate the payload and return a 400 with a meaningful message.
* Severity: **Medium**.

### B9 — `POST /tradeservice/order` is unauthenticated and bypasses all upstream validation (tests `API-05`, `API-07`, enabled)
* Product file: `trade-processor/src/main/java/finos/traderx/tradeprocessor/controller/TradeServiceController.java`, lines 17–32.
* Today: `@CrossOrigin("*")` with no authentication, no authorisation and no `@Valid`. The processor performs
  none of trade-service's ticker (reference-data) or account (account-service) checks, so anyone who can
  reach the pod can book a trade for account `999999` in ticker `NOTATICKER`, or an order with every field null.
* Should: require authentication, validate the request body, and re-run (or delegate) the ticker/account checks.
* Severity: **High** (authorisation and validation gap).

### B10 — No state-transition guard; `Cancelled` is a dead state (tests `LC-03`, `LC-04`, `LC-05`, enabled)
* Product files: `model/Trade.java` lines 69–79, `model/TradeState.java`, `TradeService.java` lines 49–71.
* Today: `setState` accepts any value from any value, so `Settled -> New` and `Cancelled -> Settled` are legal.
  `Cancelled` is declared but never produced or handled anywhere, and no cancel operation exists.
* Should: enforce the legal transition graph in one place and either implement cancellation or drop the constant.
* Severity: **Medium**.

### B11 — The `Processing` state is unobservable (test `LC-01`, enabled)
* Product file: `TradeService.java`, lines 66–71.
* Today: the trade is set to `Processing` and then to `Settled` before the second `save`, so no row is ever
  persisted or published in `Processing`. The intermediate state is pure dead code and consumers can never see it.
* Should: either persist/publish the intermediate state or remove it.
* Severity: **Low**.

### B12 — `PositionID` overrides neither `equals` nor `hashCode` (test `DB-10`, enabled)
* Product file: `trade-processor/src/main/java/finos/traderx/tradeprocessor/model/PositionID.java`.
* Today: JPA requires an `@IdClass` to implement `equals`/`hashCode`; this one uses identity semantics, so two
  logically identical keys are unequal. Persistence-context caching and any `findById(new PositionID(...))`
  based deduplication behave incorrectly.
* Should: implement `equals`/`hashCode` over both key components (and `Serializable` is already present).
* Severity: **Medium**.

### B13 — `TradeRepository` declares the wrong id type (test `DB-11`, enabled)
* Product file: `trade-processor/src/main/java/finos/traderx/tradeprocessor/repository/TradeRepository.java`, line 9.
* Today: `JpaRepository<Trade, Integer>` while `Trade.id` is a `String`. Every inherited id-based method
  (`findById`, `existsById`, `deleteById`) takes an `Integer` and can never designate a real row.
* Should: `JpaRepository<Trade, String>`.
* Severity: **Medium**.

### B14 — Zero headroom on the `SIDE` column (test `LC-06`, enabled)
* Product file: `model/Trade.java`, line 58 (`@Column(length = 4, name = "SIDE")`).
* Today: `Sell` is exactly four characters, so adding any longer side (e.g. `Short`, `SellShort`) silently
  breaks persistence. `STATE` at `length = 20` is comfortable today but has the same fragility.
* Should: size the columns generously or use an ordinal-free lookup table.
* Severity: **Low**.

## Notes

* Both disabled tests were verified to fail for the stated reason (they were run with `@Disabled` removed:
  `44 tests completed, 2 failed` — `CC-01` and `QT-07`). No product code was changed and no assertion was weakened.
* For each disabled test a companion test (`CC-02`, `QT-08`) pins the *current, buggy* behaviour so that a
  future fix will make the companion fail and force the disabled test to be re-enabled.
