# TraderX — End-to-End (Browser) Test Plan

Status: **Planning / strategy document.** No Playwright specs are added by this
change — this file defines the strategy, the exact bring-up, the golden-path
journeys, the tooling/selector conventions, the CI outline, and the concrete
spec files to be created in the follow-up implementation session.

The plan was written against a stack that was actually brought up locally with
`docker compose up` and exercised end-to-end (place a BUY, place a SELL, submit
an invalid order, reload). See [Appendix A](#appendix-a--what-was-actually-verified)
for what was verified and the workarounds required.

Today the repo has only component unit tests (Karma/Jasmine for the Angular UI,
Jest for `reference-data`). There are **no** Playwright/Cypress browser tests.
This plan targets **Playwright** against the **Angular** UI (the feature-complete
front end).

---

## 1. Full-stack bring-up (one command)

From the repository root:

```bash
docker compose up --build      # first run builds all images, then starts them
```

This builds and starts the whole polyglot stack on a single bridge network
(`localnet`). The single browser entry point is the nginx **ingress** at
**http://localhost:8080**.

### Services and ports

| Service | Tech | Container port | Host port | Ingress path (`:8080`) |
| --- | --- | --- | --- | --- |
| `ingress` | nginx | 8080 | **8080** | `/` (SPA) + reverse proxy (below) |
| `web-front-end-angular` | Angular 18 (`ng serve`) | 18093 | 18093 | `/` and `/ng-cli-ws` |
| `database` | Java + H2 (TCP/PG/Web) | 18082/18083/18084 | same | `/db-web/` |
| `reference-data` | Node/NestJS | 18085 | 18085 | `/reference-data/` |
| `trade-feed` | Node/Socket.IO | 18086 | 18086 | `/trade-feed/`, `/socket.io/` |
| `people-service` | .NET 8 | 18089 | 18089 | `/people-service/` |
| `account-service` | Java/Spring | 18088 | 18088 | `/account-service/` |
| `position-service` | Java/Spring | 18090 | 18090 | `/position-service/` |
| `trade-service` | Java/Spring | 18092 | 18092 | `/trade-service/` |
| `trade-processor` | Java/Spring | 18091 | 18091 | `/trade-processor/` |

Because the prod Angular build (`environment.prod.ts`) addresses services via
`window.location.host` + ingress path prefixes, **Playwright only needs a single
base URL: `http://localhost:8080`.** Direct service ports (e.g. `18088`,
`18090`, `18092`) are also exposed on the host and are used for the API/DB
cross-checks in the assertions below.

### Readiness — how long / what to wait for

First `docker compose up --build` is slow (it downloads the Gradle distribution,
npm packages, and the .NET SDK image and compiles all the Java services);
budget several minutes. Subsequent `up`s are much faster. **Do not gate tests on
a fixed timer** — poll HTTP readiness instead. Concrete signals observed:

- Web UI ready: `GET http://localhost:8080/` returns **HTTP 200**. Note: the
  ingress returns **502** until the Angular dev server finishes its first bundle
  (`web-front-end-angular` logs `Application bundle generation complete` and
  `➜ Local: http://localhost:18093/`). This is the last thing to come up.
- Reference data ready: `GET http://localhost:8080/reference-data/stocks/IBM`
  → `{"ticker":"IBM","companyName":"IBM"}`.
- Accounts ready: `GET http://localhost:8080/account-service/account/`
  returns the 7 seeded accounts.
- Spring services log `Started <Name>Application in … seconds`.
- `trade-feed` logs `New Connection …` / `Subscribe /trades`.

Recommended readiness gate (used by CI and the local run script):
poll `http://localhost:8080/` until 200, then poll
`http://localhost:8080/account-service/account/` and
`http://localhost:8080/reference-data/stocks/IBM` until 200.

### Database seeding & reset

- The DB is **H2, file-backed** (`jdbc:h2:./_data/traderx`, inside the
  `database` container). On **every start** of the `database` container,
  `database/run.sh` runs `org.h2.tools.RunScript` against
  [`database/initialSchema.sql`](database/initialSchema.sql), which
  `DROP`s and re-`CREATE`s `Accounts`, `AccountUsers`, `Positions`, `Trades`
  and re-inserts the sample data. So the seed is deterministic on start.
- **Reset between test runs** = restart the database service (re-runs the
  drop/create/seed script), then let the Spring services reconnect:
  `docker compose restart database` (or `docker compose down && docker compose up`
  for a clean-room run). Data written during a run (new trades/positions)
  survives a **page reload** (needed for the persistence journey) but is wiped by
  a `database` restart.

Seed data the specs rely on (from `initialSchema.sql`):

- **Accounts (7):** `10031` Internal Trading Book, `11413` Private Clients Fund
  TTXX, `22214` Test Account 20, `42422` Algo Execution Partners, `44044`
  Trading Account 1, `52355` Big Corporate Fund, `62654` Hedge Fund TXY1.
- **Deterministic test account = `22214` (Test Account 20)** with seeded
  positions `MS +1000`, `IBM -100`, `C -2000` and 3 settled trades.
- **Tickers** come from `reference-data/data/s-and-p-500-companies.csv`; the
  specs use stable ones present in both seed trades and reference data:
  **`IBM`, `MS`, `C`, `BAC`** (company names: `IBM`, `Morgan Stanley`,
  `Citigroup`, `Bank of America`).

> ⚠️ **Default selection gotcha:** on load the UI auto-selects
> `accounts[5]` = `52355` "Big Corporate Fund" (only a `BAC -2400` position),
> **not** `22214`. Every spec must **explicitly select account `22214`** from the
> dropdown before asserting — never rely on the default.

---

## 2. Golden-path journeys to automate first

Trader journey under test: **select account → look up ticker → place order via
the Create Trade Ticket modal → order flows asynchronously through
`trade-service` → `trade-feed` (Socket.IO) → `trade-processor` → DB, and streams
back to the Trade Blotter and Position Blotter over
`/accounts/{id}/trades` and `/accounts/{id}/positions`.**

Because the fill is asynchronous (trade state transitions `New → Processing →
Settled` and the position updates arrive over the socket), **every assertion
auto-retries on UI state (Playwright web-first assertions / `toPass`); never
`waitForTimeout`.**

### Key UI selectors (verbatim from the Angular templates)

| Element | Selector | Notes |
| --- | --- | --- |
| Nav tab "Trade" | `getByRole('link', { name: 'Trade' })` | route `/trade` |
| Account dropdown toggle | `app-ngx-dropdown button.dropdown-toggle` | shows selected account or `Select Account` |
| Account dropdown item | `.dropdown-menu a.dropdown-item` filtered by text | e.g. `Test Account 20` |
| Open ticket button | `#createTicketBtn` ("Create Trade Ticket") | opens modal |
| Ticket: account label | `#accountLabel` | readonly, mirrors selected account |
| Ticket: security typeahead | `#stock-input` | ngx-bootstrap typeahead on `companyName` |
| Typeahead option | `typeahead-container button.dropdown-item` filtered by text | **must click a suggestion** — `security` is only set via `typeaheadOnSelect` |
| Ticket: Buy radio | `#buyButton` (label `for="buyButton"`) | default checked |
| Ticket: Sell radio | `#sellButton` (label `for="sellButton"`) | |
| Ticket: quantity | `#quantityField` | `type=number` |
| Ticket: submit | `#createButton` ("Create") | |
| Ticket: close | `#cancelButton` ("Close") | |
| Success/error alert | `alert` component → `.alert-success` / `.alert-danger` | renders `createTicketResponse | json`, auto-dismisses after 2000ms |
| Trade Blotter grid | `app-trade-blotter .ag-theme-alpine` | ag-Grid; row id = `Trade-<id>` |
| Position Blotter grid | `app-position-blotter .ag-theme-alpine` | ag-Grid; row id = `Position-<security>` |
| ag-Grid cell (by column) | `.ag-row [col-id="security"|"quantity"|"side"|"state"]` | column fields: `security`, `quantity`, `side`, `state` |

> ngx-bootstrap/ag-Grid ids like `drpbtn0` and internal ag-Grid classes are not
> stable contracts. **Part of the implementation work is adding `data-testid`
> attributes** (see §3) — until then, target by role/text and the stable
> element `id`s above.

### 2.1 Place a BUY (primary golden path)

Steps: open `/`; select account **`22214`**; note IBM starts at **-100**; click
**Create Trade Ticket**; in `#stock-input` type `IBM` and click the `IBM`
suggestion; keep side **Buy**; set `#quantityField` = `100`; click **Create**.

UI assertions (auto-retrying):
- Trade Blotter gains a row for `IBM` with `QUANTITY=100`, `SIDE=Buy`, and
  `STATE` that settles to **`Settled`** (may briefly show `New`/`Processing` —
  assert `expect(cell).toHaveText('Settled')` so it retries through the states).
- Position Blotter `IBM` row `QUANTITY` moves **-100 → 0** (buy 100 offsets the
  short). Assert final value, not the transition.
- Optional: success `alert` (`.alert-success`) appears then auto-dismisses.

API/DB cross-check (via exposed ports):
- `GET http://localhost:18090/trades/22214` contains the new `IBM/Buy/100/Settled`
  trade.
- `GET http://localhost:18090/positions/22214` shows `IBM` quantity `0`.

### 2.2 Place a SELL

Steps: account `22214`; open ticket; select `MS` (Morgan Stanley); choose
**Sell** (`#sellButton`); quantity `100`; Create.

Assertions:
- Trade Blotter gains `MS / 100 / Sell / Settled`.
- Position Blotter `MS` moves **1000 → 900**.
- Cross-check `GET /positions/22214` → `MS` = `900`;
  `GET /trades/22214` contains the `MS/Sell/100` trade.

### 2.3 Invalid / rejected order

Two complementary cases (the UI constrains input, so cover both layers):

- **Client-side guard (UI):** open the ticket, select a valid ticker, but leave
  `#quantityField` at `0` (or empty), and click **Create**. `onCreate()` returns
  early (`!ticket.quantity`), so **no trade is submitted**: assert **no new row**
  appears in the Trade Blotter and **no success alert** shows. Also cover the
  "typed a bogus symbol but never selected a suggestion" case — `onBlur()` clears
  `security`, so Create is a no-op.
- **Server-side rejection (API-level, documented):** `trade-service` returns
  **404** for an unknown ticker or unknown account
  (`ResourceNotFoundException`). Verified:
  `POST http://localhost:18092/trade/` with `security:"ZZZZ"` → `HTTP 404`.
  Note the UI typeahead only emits **valid** tickers, and on a POST error
  `createTradeTicket` has no error handler, so the rejection is **silent** in the
  UI (no alert, no row). The follow-up spec will exercise this path by
  intercepting the network (Playwright `page.route`) to force an invalid submit
  and assert the blotter is unchanged, plus a direct API assertion for the 404.

### 2.4 Reload persists

Steps: place a BUY (as 2.1); `page.reload()`; re-select account `22214`.

Assertions:
- The new trade is **still present** in the Trade Blotter and the position value
  persists — these load via REST on account selection
  (`GET /trades/{id}`, `GET /positions/{id}` from `position-service`) before any
  socket updates, proving server-side persistence (not just live socket state).
- (This journey does **not** restart the `database` container — persistence is
  across a browser reload; a container restart intentionally resets the seed.)

---

## 3. Proposed Playwright setup

**Location & layout** (new top-level `e2e/` so browser tests are clearly
separate from the Angular unit tests under `web-front-end/angular`):

```
e2e/
  package.json                 # @playwright/test only
  playwright.config.ts         # baseURL http://localhost:8080, retries, trace/video/screenshot
  tests/
    buy-trade.spec.ts
    sell-trade.spec.ts
    invalid-order.spec.ts
    reload-persist.spec.ts
    smoke.spec.ts
  fixtures/
    app.ts                     # test fixtures: seeded account/tickers, page objects
    readiness.ts               # poll ingress + APIs before the suite
  pages/
    trade.page.ts              # Page Object: dropdown, ticket modal, blotters
  utils/
    api.ts                     # REST helpers for cross-checks & reset
```

**Config highlights (`playwright.config.ts`):**
- `use.baseURL = 'http://localhost:8080'`.
- `use.trace = 'on-first-retry'`, `screenshot = 'only-on-failure'`,
  `video = 'retain-on-failure'`.
- `retries: process.env.CI ? 2 : 0`; `expect.timeout` ~15s to absorb async fills.
- Project: Chromium first (add WebKit/Firefox later).
- `reporter: [['html'], ['list'], ['junit', { outputFile: 'results.xml' }]]`.
- Optional `webServer` block that runs the stack + readiness poll so
  `npx playwright test` boots everything on its own (see one-command below).

**Selector strategy — auto-retrying, role/testid-based:**
- Prefer `getByRole`/`getByLabel`/`getByText` and the stable element `id`s listed
  in §2. Use Playwright web-first assertions (`toBeVisible`, `toHaveText`,
  `toHaveCount`) and `expect.poll`/`toPass` for the async fills — **no fixed
  sleeps**.
- **Implementation task:** add `data-testid` to the high-value, otherwise-unstable
  elements: account dropdown + items, ticket fields, and (via ag-Grid
  `getRowId`/cell renderers) blotter rows keyed by trade id / security, e.g.
  `data-testid="position-row-IBM"`, `data-testid="trade-row-<id>"`. This is a
  small, contained UI change to be done alongside the specs.

**Deterministic data:** every spec selects account **`22214`** and uses tickers
`IBM/MS/C/BAC`. Specs are order-independent and assert **final** values; a
`database` restart (or `down && up`) provides a clean seed between full runs.

**One command to boot the stack and run the specs:**

```jsonc
// e2e/package.json
{
  "scripts": {
    // start-server-and-test waits for the ingress to answer before running
    "test:e2e": "start-server-and-test 'docker compose -f ../docker-compose.yml up --build' http-get://localhost:8080 'playwright test'",
    "test:e2e:local": "playwright test"   // assumes stack already up
  }
}
```

(Equivalently, Playwright's own `webServer: { command: 'docker compose up --build', url: 'http://localhost:8080', timeout: 600000, reuseExistingServer: !process.env.CI }`.)

---

## 4. CI workflow outline

New workflow `.github/workflows/e2e.yml`, running on a freshly started stack:

```yaml
name: e2e
on: [pull_request, workflow_dispatch]
jobs:
  playwright-e2e:
    runs-on: ubuntu-latest              # docker + compose preinstalled
    timeout-minutes: 40
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - name: Build & start full stack
        run: docker compose up --build -d
      - name: Wait for readiness
        run: |                          # poll ingress + key APIs, fail fast on timeout
          npx wait-on -t 600000 \
            http-get://localhost:8080 \
            http-get://localhost:8080/account-service/account/ \
            http-get://localhost:8080/reference-data/stocks/IBM
      - name: Install Playwright
        working-directory: e2e
        run: npm ci && npx playwright install --with-deps chromium
      - name: Run E2E specs
        working-directory: e2e
        run: npx playwright test
      - name: Dump service logs on failure
        if: failure()
        run: docker compose logs --no-color > compose-logs.txt
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: e2e-artifacts
          path: |
            e2e/playwright-report/
            e2e/test-results/
            compose-logs.txt
      - name: Tear down
        if: always()
        run: docker compose down -v
```

Notes: runners need enough disk/RAM for the full polyglot build (this stack is
large — see Appendix A on disk pressure). Alternatives to `wait-on`:
`start-server-and-test`, or per-service Docker Compose `healthcheck`s so
`up --wait` blocks until healthy. Caching Gradle/npm/NuGet layers will cut the
build time materially.

---

## 5. Spec files to add in the follow-up implementation session

| File | Journey (§) | Core assertions |
| --- | --- | --- |
| `e2e/tests/smoke.spec.ts` | bring-up | `/` loads, header + Trade/Account tabs render, account dropdown lists 7 accounts, seeded blotters render for `22214` |
| `e2e/tests/buy-trade.spec.ts` | 2.1 | BUY 100 IBM on `22214` → blotter row `IBM/100/Buy/Settled`; position `IBM` -100→0; API cross-check |
| `e2e/tests/sell-trade.spec.ts` | 2.2 | SELL 100 MS on `22214` → blotter row `MS/100/Sell`; position `MS` 1000→900; API cross-check |
| `e2e/tests/invalid-order.spec.ts` | 2.3 | qty 0 / unselected ticker → no row, no success alert; forced-invalid submit unchanged; direct API 404 |
| `e2e/tests/reload-persist.spec.ts` | 2.4 | after BUY + reload + reselect `22214`, trade + position persist (loaded via REST) |

Supporting (non-spec) files created alongside: `e2e/playwright.config.ts`,
`e2e/package.json`, `e2e/pages/trade.page.ts`, `e2e/fixtures/*.ts`,
`e2e/utils/api.ts`, plus the `data-testid` UI additions from §3 and
`.github/workflows/e2e.yml`.

---

## Appendix A — What was actually verified

The stack was brought up with `docker compose up --build` and exercised:

- All 10 services reached `Up`; **web UI served HTTP 200 at
  http://localhost:8080** (Angular Trade view rendered with header, account
  dropdown, and Trades/Positions blotters).
- `GET /account-service/account/` → 7 seeded accounts;
  `GET /reference-data/stocks/IBM` → `{"ticker":"IBM","companyName":"IBM"}`;
  `GET /position-service/positions/22214` and `/trades/22214` → seeded data.
- **BUY 50 IBM on `22214`** via `POST /trade-service/trade/` → trade flowed
  through `trade-feed`/`trade-processor`, appeared in `/trades/22214` as
  `Settled`, and position `IBM` moved **-100 → -50** (async fill confirmed).
- **Invalid ticker** `POST /trade/ {security:"ZZZZ"}` → **HTTP 404**
  (`ResourceNotFoundException`) — the rejection path exists.

### Workarounds required to bring the stack up (to fix before/with the E2E work)

These are **pre-existing issues on `main`**, documented here (this PR does not
change them):

1. **`reference-data` (and other Node) `npm install` fails** with an `ERESOLVE`
   peer-dependency conflict (`@nestjs/schematics@11` wants `prettier@^3`, root
   pins `prettier@^2`). Workaround used locally: `legacy-peer-deps=true`
   (`.npmrc`) / `npm install --legacy-peer-deps`. Recommend committing a
   `.npmrc` or a lockfile fix, or aligning the prettier/nestjs versions.
2. **Angular container serves the *production* build** (`Dockerfile` ENTRYPOINT
   is `npm run start-prod`, in shell form, so the compose `command: npm run start`
   is ignored). The prod build **fails to compile** with `TS2345` in
   `web-front-end/angular/main/app/accounts/user/assign-user.component.ts`
   (`switchMap<string, …>` over a `string | undefined` source), so the ingress
   returns **502** until fixed. Workaround used locally: correct the generic to
   `switchMap<string | undefined, …>`. Recommend fixing this type error (it also
   blocks any production build).
3. **Disk/resources:** building the full polyglot stack is heavy (Gradle + npm +
   .NET). The build initially failed with *"No space left on device"*; freeing
   space (and `docker system prune`) resolved it. CI runners must be sized
   accordingly, and Gradle/npm/NuGet caching is recommended.
