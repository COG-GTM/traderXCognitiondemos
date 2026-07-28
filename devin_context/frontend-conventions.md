# Frontend conventions — `web-front-end/angular`

Angular 18, TypeScript 5.4, Bootstrap 5.3, ngx-bootstrap, AG Grid 32, Socket.IO. No Tailwind, no
component library beyond the above, no CSS-in-JS.

## 1. Project layout

```
main/app/
  <feature>/                       # trade/, accounts/, header/, dropdown/
    <feature>.component.{ts,html,scss,spec.ts}
    <feature>.module.ts            # feature NgModule, declares + exports its components
    <child-component>/             # one folder per child component
  service/                         # *.service.ts, providedIn: 'root'
  model/                           # *.model.ts — interfaces + enums only
  test-utils/                      # mocks.service.ts, utils.ts (faker builders)
```

- One component per folder, four files, kebab-case names (`position-summary.component.ts`).
- Class names are PascalCase and end in `Component` / `Service`; selectors are prefixed `app-`.
- Register new components in the owning feature module's `declarations` (and `exports` only if
  another module needs them). These are **NgModule-based, not standalone** components.

## 2. Component conventions

- Templates and styles always live in separate files (`templateUrl` / `styleUrls`), never inline.
- Account-scoped widgets take the account as an input and react to it, following the blotters:

  ```ts
  @Input() account?: Account;

  ngOnChanges(change: SimpleChanges) {
    if (change.account?.currentValue && change.account.currentValue !== change.account.previousValue) {
      // fetch for change.account.currentValue.id, then (re)subscribe to the feed
    }
  }
  ```

- Live data comes from `TradeFeedService.subscribe(topic, cb)`, which returns an unsubscribe
  function. Store it, call the previous one before resubscribing, and call it in `ngOnDestroy`:

  ```ts
  this.socketUnSubscribeFn?.();
  this.socketUnSubscribeFn = this.tradeFeed.subscribe(`/accounts/${accountId}/positions`, (data) => …);
  ```

- HTTP goes through the existing services in `main/app/service` (`PositionService`,
  `AccountService`, `SymbolService`, …). Components never call `HttpClient` directly and never
  hard-code URLs — service URLs come from `main/environments/environment*.ts`.
- Keep derived/aggregate values as plain class fields recomputed in one small `private` method
  (e.g. `recalculate()`), not as getters called from the template.
- Public API of a component: `@Input()`/`@Output()` first, then fields used by the template, then
  lifecycle hooks, then handlers, then private helpers.

## 3. Templates

- Bootstrap 5 utility classes for layout and spacing (`d-flex`, `gap-3`, `mb-4`, `p-5`); custom
  SCSS only for what utilities cannot express.
- Section headings inside a panel use `<h5>` (see `Trades` / `Positions` blotters).
- Structural directives stay simple: `*ngIf` / `*ngFor` with `trackBy` for lists that update live.
- Give interactive elements an `id` when a test or demo needs to target them
  (`id="createTicketBtn"`).

## 4. SCSS

- One `.scss` per component, scoped by a single top-level class matching the component
  (`.position-summary { … }`), nested with `&-` suffixes rather than deep selectors.
- Use the tokens in [`design-system.md`](design-system.md) — never invent new hex values.
- Do not use `::ng-deep` or `!important`. Global overrides belong in `main/styles.scss`.

## 5. Tests

- Every component has a `.spec.ts` next to it, using `TestBed.configureTestingModule` with the
  mock services from `main/app/test-utils/mocks.service.ts` (`MockTradeService`,
  `MockTradeFeedService`, `MockAccountService`, …) — never the real HTTP services.
- Cover at minimum: it creates, it fetches on `ngOnChanges` for a new account, and the rendered
  output for that data.
- Run before opening a PR, from `web-front-end/angular`:

  ```bash
  npm run test:ci        # Karma + ChromeHeadlessNoSandbox
  npm run build
  ```

  `npm run lint` is currently a no-op on Angular 18 (the `tslint` builder no longer exists); do not
  spend time trying to make it run — the compiler and the test build are the gate.

## 6. Things we do not do

- No new runtime dependencies for styling or state (no Tailwind, Material, NgRx, moment).
- No `any` in new code where a model interface exists in `main/app/model`.
- No business logic in templates and no formatting logic duplicated across components — use
  Angular pipes (`number`, `date`) instead.
- No changes to the React reference app in `web-front-end/react` unless the task asks for it.
