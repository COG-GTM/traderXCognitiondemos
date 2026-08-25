# TraderX — Angular front-end test report (`TEST_REPORT_ui.md`)

Branch: `devin/swarm-ui-tests`

Scope: Karma/Jasmine unit + component tests for `web-front-end/angular` (Part A, runs in CI) and
Playwright browser specs under `web-front-end/angular/e2e` (Part B, excluded from CI).

No Angular component/service/template production code was modified.

## How to run

```bash
cd web-front-end/angular
npm install                 # repo has no package-lock.json, so `npm ci` cannot be used
npm run test:ci             # Karma / Jasmine, ChromeHeadlessNoSandbox
npm run test:e2e:typecheck  # typecheck for the Playwright specs
npm run test:e2e            # Playwright — needs the full stack (docker-compose up)
```

Final Karma result:

```
Chrome Headless 107.0.5296.0 (Linux 0.0.0): Executed 94 of 101 (skipped 7) SUCCESS (15.786 secs / 15.657 secs)
```

**94 specs pass, 7 specs are disabled (`xit`) because they fail on genuine product bugs, 0 failures.**

Notes on pre-existing repository issues encountered (not fixed, out of scope):

- `npm ci` cannot be used — there is no `package-lock.json` in the repo.
- `npm run lint` fails before running anything: `Cannot find builder "@angular-devkit/build-angular:tslint"`
  (`angular.json` still points at the TSLint builder, which no longer ships with Angular 18).
  Static verification for this change was therefore done with `tsc --noEmit` for both the spec
  project (`tsconfig.spec.json`) and the e2e project (`e2e/tsconfig.json`); both are clean.
- With `rxjs` resolving to 7.8.2, `main/app/accounts/user/assign-user.component.ts` no longer
  compiles (`OperatorFunction<string, User[]>` vs `OperatorFunction<string | undefined, User[]>`).
  `package.json` was left as-is (`^7.8.1`); tests were run against 7.8.1.

---

## Part A — Karma / Jasmine test cases

All previously `xdescribe`-d suites are enabled. Everything below is enabled and green unless the
ID is marked *disabled*.

### Trade ticket — `main/app/trade/trade-ticket/trade-ticket.component.spec.ts`

| ID | Area | Edge / corner case | Expected behaviour asserted |
|----|------|--------------------|------------------------------|
| UI-TT-01 | trade ticket | component construction | component is created |
| UI-TT-02 | trade ticket | initial render | account label, empty security, `Buy` preselected, empty quantity |
| UI-TT-03 | trade ticket | happy path create | ticket built from inputs and emitted through `create` |
| UI-TT-04 | trade ticket | cancel | `cancel` emitted on Close |
| UI-TT-05 | trade ticket | typeahead seeding | `filteredStocks` seeded from the `stocks` input on init |
| UI-TT-06 | trade ticket | **quantity 0** | `onCreate()` returns silently, nothing is emitted, and **no user-visible validation feedback is rendered** (asserted: no `.invalid-feedback`/`.alert` in the DOM) |
| UI-TT-07 | trade ticket | negative quantity (-5) | emitted unvalidated |
| UI-TT-08 | trade ticket | fractional quantity (1.5) | emitted unvalidated |
| UI-TT-09 | trade ticket | quantity typed as a string | `ngModel` on `type=number` coerces to a number before emit |
| UI-TT-10 | trade ticket | non-numeric text in the number field | value becomes `null`, `onCreate()` refuses to emit |
| UI-TT-11 | trade ticket | quantity `2147483648` (> Java `int`) | emitted with no client-side guard (server-side overflow risk) |
| UI-TT-12 | trade ticket | empty security | nothing emitted |
| UI-TT-13 | trade ticket | whitespace-only security | emitted verbatim — the value is never trimmed |
| UI-TT-14 | trade ticket | free-typed unknown ticker + blur | `selectedCompany` is truthy so `onBlur()` does **not** clear it, `ticket.security` stays empty and create silently does nothing |
| UI-TT-15 | trade ticket | blur with nothing typed/selected | `ticket.security` cleared |
| UI-TT-16 | trade ticket | typeahead selection | ticket takes the ticker, not the company name |
| UI-TT-17 | trade ticket | `account` input `undefined` | `accountId` defaults to `0` and a trade can still be submitted against account 0 |
| UI-TT-18 | trade ticket | empty `stocks` input | renders, typeahead has no options |
| UI-TT-19 | trade ticket | undefined `stocks` input | `filteredStocks` stays undefined, no crash |
| UI-TT-20 | trade ticket | side toggle | Buy → Sell → Buy round trip reflected in the ticket |
| UI-TT-21 | trade ticket | state after create | ticket is **not** reset after a successful create (documented behaviour) |
| UI-TT-22 | trade ticket | rapid double click on Create | two identical tickets are emitted — no duplicate-submission guard |
| UI-TT-23 | trade ticket | cancel | `cancel` emitted and ticket state untouched |

### Trade blotter — `main/app/trade/trade-blotter/trade-blotter.component.spec.ts`

| ID | Area | Edge / corner case | Expected behaviour asserted |
|----|------|--------------------|------------------------------|
| UI-TB-01 | trade blotter | construction | component created |
| UI-TB-02 | trade blotter | column defs | SECURITY / QUANTITY / SIDE / STATE rendered |
| UI-TB-03 | trade blotter | `ngOnChanges` | `getTrades(accountId)` called, trades stored |
| UI-TB-04 | trade blotter | feed subscription | subscribes to `/accounts/{id}/trades` |
| UI-TB-05 | trade blotter | row ids | `getRowId` returns `Trade-<id>` |
| UI-TB-06 | trade blotter | empty data set | empty AG Grid renders with 0 rows and no error |
| UI-TB-07 *(disabled)* | trade blotter | feed message for a **different** accountId | should be ignored — **LATENT BUG 1** |
| UI-TB-08 | trade blotter | same, observed behaviour | foreign-account trade is added to the grid |
| UI-TB-09 *(disabled)* | trade blotter | update for an existing trade id | should update the row in place — **LATENT BUG 2** |
| UI-TB-10 | trade blotter | same, observed behaviour | lookup uses the raw id while rows are keyed `Trade-<id>`, so it never matches and a second row is added |
| UI-TB-11 *(disabled)* | trade blotter | out-of-order update (`Settled` then `New`) | should not regress to the earlier state — **LATENT BUG 3** |
| UI-TB-12 | trade blotter | same, observed behaviour | duplicate rows, stale `New` state visible |
| UI-TB-13 | trade blotter | partial payload (no `quantity`, no `state`) | row added with blank cells, no crash |
| UI-TB-14 | trade blotter | payload with no `id` | row added with an undefined id, no crash |
| UI-TB-15 *(disabled)* | trade blotter | `null` payload | should be ignored — **LATENT BUG 4** |
| UI-TB-16 | trade blotter | same, observed behaviour | a `null` frame throws out of the feed callback |
| UI-TB-17 | trade blotter | account switch | previous account topic is unsubscribed (leak/cross-talk check) |
| UI-TB-18 | trade blotter | destroy | subscription torn down in `ngOnDestroy` |
| UI-TB-19 | trade blotter | message for the account switched away from | callback no longer wired, blotter unaffected |
| UI-TB-20 | trade blotter | unicode + 200-char ticker | rendered verbatim |
| UI-TB-21 | trade blotter | HTML in a security name | escaped as text, no injected element (XSS check) |
| UI-TB-22 | trade blotter | trades request that never answers | live updates are queued, no crash |
| UI-TB-23 *(disabled)* | trade blotter | trades request never answers / fails | should leave the pending state — **LATENT BUG 5** |
| UI-TB-24 | trade blotter | same, observed behaviour | blotter stays pending forever, updates never applied |

### Position blotter — `main/app/trade/position-blotter/position-blotter.component.spec.ts`

| ID | Area | Edge / corner case | Expected behaviour asserted |
|----|------|--------------------|------------------------------|
| UI-PB-01 | position blotter | construction | component created |
| UI-PB-02 | position blotter | grid contents | positions rendered |
| UI-PB-03 | position blotter | empty data set | empty grid, no error |
| UI-PB-04 | position blotter | quantity 0 | rendered as `0` |
| UI-PB-05 | position blotter | negative (short) quantity | rendered as `-25` |
| UI-PB-06 *(disabled)* | position blotter | new quantity for the same security | should update the row in place — **LATENT BUG 6** |
| UI-PB-07 | position blotter | same, observed behaviour | lookup by raw `security` never matches the `Position-<security>` row key, duplicate row added |
| UI-PB-08 | position blotter | partial payload (no quantity) | no crash, blank cell |
| UI-PB-09 *(disabled)* | position blotter | `null` payload | should be ignored — **LATENT BUG 7** |
| UI-PB-10 | position blotter | same, observed behaviour | throws out of the feed callback |
| UI-PB-11 | position blotter | account switch | previous `/accounts/{id}/positions` subscription torn down |
| UI-PB-12 | position blotter | destroy | unsubscribes |
| UI-PB-13 | position blotter | unicode / very long security / HTML | rendered verbatim and HTML-escaped |
| UI-PB-14 | position blotter | positions request fails (HTTP 500) | component leaves the pending state and still applies later live updates |

### Services

| ID | Area | Edge / corner case | Expected behaviour asserted |
|----|------|--------------------|------------------------------|
| UI-PS-01 | `PositionService` | 404 from `getTrades` | error notification surfaced to the caller |
| UI-PS-02 | `PositionService` | 500 from `getPositions` | error notification surfaced |
| UI-PS-03 | `PositionService` | connection failure / timeout (status 0) | error notification surfaced |
| UI-PS-04 | `PositionService` | failed trades request | no retry is issued |
| UI-PS-05 | `PositionService` | account id `0` / negative | id kept verbatim in the URL |
| UI-PS-06 | `PositionService` | empty trade list | passed through unchanged |
| UI-SS-01 | `SymbolService` | repeated 500 on `getStocks` | request is retried before giving up |
| UI-SS-02 | `SymbolService` | 400 from `createTicket` | error propagated to the caller |
| UI-SS-03 | `SymbolService` | quantity `2147483648` | posted unchanged (no client-side clamping) |
| UI-SS-04 | `SymbolService` | connection failure on `createTicket` | error propagated |
| UI-FS-01 | `TradeFeedService` | topic routing | callback only fires for its own topic |
| UI-FS-02 | `TradeFeedService` | frames published by `System` | dropped |
| UI-FS-03 | `TradeFeedService` | unsubscribe function | stops delivery |
| UI-FS-04 | `TradeFeedService` | two topics, one torn down | the other stays alive |
| UI-FS-05 | `TradeFeedService` | publish frame with no envelope | throws (documented behaviour) |

### Existing suites re-enabled (previously `xdescribe`)

`app.component.spec.ts` (4), `header.component.spec.ts` (1), `trade.component.spec.ts` (4),
`accounts/account.component.spec.ts` (5), `accounts/edit/edit.component.spec.ts` (6),
`accounts/user/assign-user.component.spec.ts` (5, one added), plus the trade ticket / blotter suites above.

Test-side fixes that were needed to make them pass (no production code touched):

- `MockUserService.getUsers` returned `{ people: [...] }`, but the real `UserService.getUsers`
  maps that to `User[]`. The mock now returns `User[]` so the typeahead spec exercises the real contract.
- Stale API usage: the old blotter spec called `component.getRowNodeId(...)`, which no longer exists
  (Angular/AG Grid 32 renamed it to `getRowId`).
- AG Grid renders asynchronously; a `settle()` helper (`main/app/test-utils/utils.ts`) runs change
  detection until the grid api and DOM are usable, replacing the fragile single `detectChanges()`.
- New mocks in `main/app/test-utils/mocks.service.ts`: a subscription-aware `MockTradeFeedService`
  (records topics, allows `emit`), `MockFailingPositionService` (HTTP 500) and
  `MockHangingPositionService` (request that never answers) so no test needs a backend.
- The `should allow to search user` spec drives `users$` directly instead of typing into the
  typeahead input: the component only samples `this.search` at subscription time (see LATENT BUG 8),
  so a DOM-driven assertion can never pass without changing production code.

---

## Latent bugs surfaced

### 1. Trade blotter accepts feed messages for other accounts

- Disabled test: `LATENT BUG: should ignore a trade-feed payload belonging to a different accountId`
- Product file: `web-front-end/angular/main/app/trade/trade-blotter/trade-blotter.component.ts:52-55`
- Today: the callback applies every payload delivered on the subscribed topic without checking
  `data.accountid` against the selected account. Any mis-routed/replayed frame (the socket layer
  filters on the topic string only) shows up in the wrong user's blotter.
- Should: ignore payloads whose `accountid` does not match `this.account.id`.
- Severity: **High** (cross-account data leakage in a trading blotter).

### 2. Trade row updates never match — row id prefix mismatch

- Disabled test: `LATENT BUG: should update an existing trade row in place when a later state arrives`
- Product file: `trade-blotter.component.ts:80` (`this.gridApi.getRowNode(data.id)`) vs `:66`
  (`getRowId` returns `Trade-${params.data.id}`)
- Today: rows are keyed `Trade-<id>` but looked up by the bare `<id>`, so `getRowNode` always
  returns `undefined` and every state transition adds a **duplicate row** instead of updating one.
- Should: look the node up with the same key the grid was given (`Trade-${data.id}`).
- Severity: **High** (the blotter shows N rows per trade, one per state transition).

### 3. Out-of-order trade updates leave a stale state visible

- Disabled test: `LATENT BUG: should not regress to an earlier state when an out-of-order update arrives`
- Product file: `trade-blotter.component.ts:79-102`
- Today: a consequence of bug 2 plus the absence of any sequencing/`updated`-timestamp check —
  a `Settled` frame followed by a late `New` frame for the same trade id leaves both rows in the
  grid, with the stale `New` row on top (`addIndex: 0`).
- Should: apply the newest state for a trade id and ignore older frames.
- Severity: **High** (a settled trade can be displayed as unsettled).

### 4. `null` trade-feed payload throws

- Disabled test: `LATENT BUG: should ignore a null trade-feed payload instead of throwing`
- Product file: `trade-blotter.component.ts:52-55` / `:104-110`
- Today: `updateTrades(null)` reaches `getRowNode(data.id)` and throws a `TypeError` inside the
  socket callback.
- Should: ignore null/undefined frames.
- Severity: **Medium** (an unhandled error in the socket callback; subsequent frames on that
  emitter can be lost).

### 5. Trade blotter never leaves the pending state when the trades request fails or hangs

- Disabled test: `LATENT BUG: should stop showing the blotter as pending when the trades request never answers or fails`
- Product file: `trade-blotter.component.ts:47-50`
- Today: `getTrades(...).subscribe(next)` has **no error callback and no timeout**; `isPending`
  is only cleared in the success path, so on a 500/timeout the blotter stays `isPending = true`
  forever and every live update is queued into `pendingTrades` and never rendered.
  (The position blotter does have an error callback — `position-blotter.component.ts:46-48`.)
- Should: clear the pending state and surface the failure, as the position blotter does.
- Severity: **High** (silent permanent blank blotter after one failed request).

### 6. Position row updates never match — row id prefix mismatch

- Disabled test: `LATENT BUG: should update a position row in place when a new quantity arrives for the same security`
- Product file: `position-blotter.component.ts:74` (`getRowNode(data.security)`) vs `:100`
  (`getRowId` returns `Position-${params.data.security}`)
- Today: same mismatch as bug 2 — each position update adds a duplicate row, so a security can
  appear several times with different quantities and the user cannot tell which is current.
- Should: look up `Position-${data.security}`.
- Severity: **High** (wrong/ambiguous positions displayed).

### 7. `null` position payload throws

- Disabled test: `LATENT BUG: should ignore a null position payload instead of throwing`
- Product file: `position-blotter.component.ts:52-55` / `:65-71`
- Today: `update(null)` throws on `data.security`.
- Should: ignore null/undefined frames.
- Severity: **Medium**.

### 8. User search never re-queries when the search text changes

- Not a disabled test — recorded here because it forced a test-side workaround.
- Product file: `web-front-end/angular/main/app/accounts/user/assign-user.component.ts` (`ngOnInit`,
  `this.users$ = new Observable(observer => observer.next(this.search))`)
- Today: the observable emits the value of `this.search` **once at subscription time** and then
  never completes/emits again. ngx-bootstrap's async typeahead subscribes once per keystroke in
  some versions only; with this implementation the suggestion list cannot track what is typed
  reliably, and no `observer.complete()` is ever called (the observable leaks).
- Should: emit on every change of `search` (e.g. bind the typeahead to a `Subject` fed from the
  input, or use ngx-bootstrap's `typeaheadOnBlur`/value stream) and complete the observable.
- Severity: **Medium** (user search in account administration is unreliable).

### 9. Trade ticket rejects quantity `0` (and any invalid input) with no user feedback

- Not disabled — the observed behaviour is asserted in `UI-TT-06`.
- Product file: `trade-ticket.component.ts:46-50`
- Today: `if (!this.ticket.security || !this.ticket.quantity) { console.warn(...); return; }`.
  Quantity `0` is falsy, so the create button appears to do nothing at all; the only signal is a
  console warning. Nothing in the DOM tells the user why.
- Should: show validation feedback, and treat `0` as an explicit invalid-quantity error rather
  than as "not set".
- Severity: **Medium** (silent failure on a primary action).

### 10. No client-side quantity validation at all

- Not disabled — asserted in `UI-TT-07`, `UI-TT-08`, `UI-TT-11`.
- Product file: `trade-ticket.component.ts:46-53`
- Today: negative, fractional and out-of-`int`-range quantities (`2147483648`) are emitted and
  posted verbatim. `trade-service` maps `quantity` to a Java `int`, so `2147483648` overflows /
  is rejected server-side only.
- Should: validate sign, integrality and range before submitting.
- Severity: **Medium**.

### 11. No duplicate-submission guard on the trade ticket

- Not disabled — asserted in `UI-TT-22`.
- Product file: `trade-ticket.component.ts:46-53`
- Today: a double click on Create emits two identical tickets; the ticket is also not reset after
  a create, so the same order can be resubmitted repeatedly.
- Should: disable the button while a create is in flight (and/or reset the form).
- Severity: **Medium** (duplicate trades).

---

## Part B — Playwright end-to-end specs (not run in CI)

Files: `web-front-end/angular/e2e/*.spec.ts`, shared helpers in `e2e/helpers.ts`,
`web-front-end/angular/playwright.config.ts` (`baseURL: process.env.TRADERX_URL ?? 'http://localhost:18093'`),
`e2e/tsconfig.json` for typechecking, `@playwright/test` pinned to `1.62.1` (released 2026-07-30).

| ID | Spec | Case |
|----|------|------|
| E2E-01 | `trade-ticket.spec.ts` | submit a trade ticket and see the trade blotter gain a row |
| E2E-02 | `trade-ticket.spec.ts` | quantity 0 — nothing is submitted and no validation message appears |
| E2E-03 | `trade-ticket.spec.ts` | free-typed unknown ticker, blur, create refused |
| E2E-04 | `trade-ticket.spec.ts` | Buy/Sell toggle preserved through submission |
| E2E-05 | `trade-ticket.spec.ts` | cancel closes the ticket and books nothing |
| E2E-06 | `trade-ticket.spec.ts` | double click on Create books two trades |
| E2E-07 | `trade-ticket.spec.ts` | quantity `2147483648` accepted by the client |
| E2E-08 | `blotters.spec.ts` | both blotters render empty before an account is chosen |
| E2E-09 | `blotters.spec.ts` | switching accounts replaces the blotter content |
| E2E-10 | `blotters.spec.ts` | position blotter updates after a buy is booked |
| E2E-11 | `trade-feed-reconnect.spec.ts` | UI stays usable and trades still arrive after the feed websocket is dropped |
| E2E-12 | `trade-feed-reconnect.spec.ts` | state after a full page reload |

These require the whole stack (`docker-compose up` at the repo root) and are intentionally not
wired into any CI workflow.
