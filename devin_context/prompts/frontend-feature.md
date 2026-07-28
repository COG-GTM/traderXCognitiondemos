# Prompt: build a frontend feature under our conventions

Copy from the line below into a Devin session on this repo.

---

Before writing any code, read `devin_context/README.md` and every doc under
`devin_context/frontend/`. They are the normative frontend conventions for this repo — stack and
dependency policy, the Bootstrap/AG Grid design system, component patterns, service and RxJS
rules, spec and `id`-selector conventions, and the pre-PR checklist. Follow them exactly, and
mirror the golden files they point to instead of inventing new patterns. Then open the files those
docs name for the area you're touching and match them.

[feature] Add a **Positions** tab to the Angular UI at `web-front-end/angular`: a new route
`/positions` with a tab in the header, an account selector, and an AG Grid blotter of the selected
account's positions (`SECURITY`, `QUANTITY`, `UPDATED`) that stays live off the trade feed. Above
the grid, show the account's distinct-security count and total absolute quantity as Bootstrap
cards. Everything must come from the existing services in `main/app/service/` — do not add an
endpoint, and do not display any figure the API can't supply.

Constraints:

- No new frontend dependency. Bootstrap 5, ngx-bootstrap, AG Grid, RxJS and socket.io-client are
  what you have.
- NgModule-based Angular 18, `*ngIf` / `*ngFor` — this app does not use standalone components or
  the new control-flow syntax.
- Container owns the services; children take `@Input()`s and emit `@Output()`s.
- Store and call the trade-feed teardown function on input change and in `ngOnDestroy`.
- Add a `<name>.component.spec.ts` beside every new component, using the mocks in
  `main/app/test-utils/mocks.service.ts`.
- Give every new interactive control and grid a stable `id`.

Before opening the PR, run `npm --prefix web-front-end/angular run build` and
`npm --prefix web-front-end/angular run test:ci`, bring the stack up (`docker compose up`, UI on
`:8080`), exercise the new tab in a browser — including creating a trade and watching it land —
and put a screenshot in the PR. Paste `devin_context/frontend/review-checklist.md` into the PR
description with every box ticked; if a box can't be ticked, explain why instead of dropping it.
