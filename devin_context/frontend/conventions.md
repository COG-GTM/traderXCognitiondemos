# Frontend conventions

Applies to `web-front-end/angular`. Paths below are relative to that directory.

## Stack rules

- **Angular 18, NgModule-based.** This app does not use standalone components, signals, or the
  new control-flow syntax (`@if` / `@for`). Declare components in a feature `NgModule` and use
  `*ngIf` / `*ngFor`. Don't half-migrate the app as a side effect of a feature.
- **TypeScript 5.4, `strict: true`** (`@schematics/angular:application` strict, `tsconfig`).
  `npm run build` runs the production AOT build and must pass.
- **No new dependencies.** The UI is deliberately vanilla — that is the point of TraderX. What's
  already there and sufficient:
  | Need | Use |
  | --- | --- |
  | Layout, spacing, typography, buttons, forms | **Bootstrap 5** utility classes |
  | Modals, alerts, dropdowns, typeahead | **ngx-bootstrap** |
  | Any tabular data | **AG Grid** (`ag-grid-angular`, community) |
  | Async, streams, derived state | **RxJS 7** |
  | Live updates | **socket.io-client** via `TradeFeedService` |
  | HTTP | `@angular/common/http` |
  No Material, no PrimeNG, no Tailwind, no NgRx, no lodash, no moment/date-fns, no chart library.
  If you truly need one, say so in the PR and wait — don't just install it.
- **No `any` where a model exists.** `main/app/model/*.model.ts` is the place to add or extend a
  type. (The existing code has some `any` in generic plumbing — `dropdown.component.ts`,
  cell-renderer params, `createTicketResponse`. Don't copy it into new domain code.)
- **Never edit `web-front-end/react` to satisfy an Angular task**, and vice versa.

## Commands

```bash
cd web-front-end/angular
npm install
npm start                     # dev server on :18093 (WEB_SERVICE_PORT)
npm run build                 # production AOT build — the gate
npm run test:ci               # Karma/Jasmine, ChromeHeadlessNoSandbox, single run — the other gate
```

`npm run lint` **does not work**: `angular.json` still points at
`@angular-devkit/build-angular:tslint`, a builder removed in Angular 12, and `main/tslint.json`
extends a file that no longer exists. Nothing is linted today, so the selector rules below are
conventions you have to keep by hand, not something a tool enforces. Don't count a green lint as
evidence, and don't rewire the linter as a side effect of a feature.

The whole stack (all services + this UI on `:8080`) comes up with `docker compose up` from the
repo root. Ports for running services by hand are in the root `README.md`.

## File layout

```text
web-front-end/angular/main/
  app/
    app.component.ts|html|scss     # shell: header + <router-outlet>
    app.module.ts                  # root module
    routing.ts                     # all routes, one place
    page-not-found.component.ts
    model/                         # interfaces + enums only, one file per domain area
      account.model.ts  trade.model.ts  symbol.model.ts  user.model.ts
    service/                       # injectable data access, one file per backend service
      account.service.ts  position.service.ts  symbols.service.ts
      user.service.ts  trade-feed.service.ts  theme.service.ts
    <feature>/                     # e.g. trade/, accounts/
      <feature>.module.ts
      <feature>.component.ts|html|scss
      <child>/<child>.component.ts|html|scss
    dropdown/                      # cross-feature presentational components
    test-utils/mocks.service.ts
  environments/                    # environment.ts | .local.ts | .prod.ts — service URLs only
  styles.scss                      # global styles: 3 imports + a few overrides
```

A new screen means: a folder under `main/app/<feature>/`, a module, a route in `routing.ts`, and a
nav entry in `header/header.component.html`. Nothing else moves.

## Naming

| Thing | Rule | Example |
| --- | --- | --- |
| Component selector | element, `app-` prefix, kebab-case (`tslint.json` states it; nothing enforces it) | `app-trade-blotter` |
| Directive selector | attribute, `app` prefix, camelCase | `appHighlight` |
| Files | `<name>.component.ts` / `.html` / `.scss`, `<name>.service.ts`, `<name>.model.ts` | `trade-ticket.component.ts` |
| Class | PascalCase, suffixed | `TradeBlotterComponent`, `PositionService` |
| Spec | `<name>.spec.ts` beside the file it tests | `account.component.spec.ts` |
| Observable field | trailing `$` | `accounts$`, `users$` |
| Output | verb, no `on` prefix; the handler that receives it *is* `onX` | `@Output() create` → `(create)="createTradeTicket($event)"` |
| Domain enum | PascalCase members, string values matching the backend | `State.Settled = 'Settled'` |

## Code style

- 4-space indent in `main/app/**` (some older child components use 2 — match the file you're in).
- Single quotes, semicolons.
- Templates live in `templateUrl` files, not inline — except a one-line renderer like
  `button-renderer.component.ts`, where an inline `template` is the established exception.
- Constructor parameter injection with access modifiers: `constructor(private http: HttpClient) { }`.
- Prefer `readonly`/`private` for fields the template doesn't touch.
- `console.log` is used liberally in this codebase for the demo narrative. Don't add new ones in
  hot paths (grid transactions, feed callbacks); a single log per user-initiated action matches
  the house style.
