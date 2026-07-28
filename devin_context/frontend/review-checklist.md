# Pre-PR review checklist

Paste this into the PR description with every box ticked. If a box can't be ticked, say why in the
PR rather than dropping the line.

## Build and test

- [ ] `npm --prefix web-front-end/angular run build` passes (production AOT).
- [ ] `npm --prefix web-front-end/angular run test:ci` passes, and every new component has a spec.
- [ ] `web-front-end/angular/package.json` is unchanged — no dependency added.
- [ ] Nothing under `web-front-end/react` was touched (unless the task named it).

## Structure

- [ ] New screen = folder under `main/app/<feature>/` + feature module + route in `routing.ts` +
      tab in `header/header.component.html`.
- [ ] Component declared in exactly one `NgModule`; only the ngx-bootstrap modules actually used
      are imported.
- [ ] Containers own services and data; children take `@Input()` / emit `@Output()` and inject
      nothing.
- [ ] New/extended types live in `main/app/model/*.model.ts`; no `any` in domain code.

## Design system

- [ ] Bootstrap 5 utilities only — no new hex value, no font-size, no spacing scale.
- [ ] Buttons follow the semantics: `btn-primary` primary action (one per section), `btn-info`
      secondary/row action, `btn-secondary` cancel; all `btn-sm`.
- [ ] Any SCSS added is component-scoped, nested under one feature root class, layout-only.
- [ ] Inline `style` used only for AG Grid box sizing.
- [ ] Uses Bootstrap contextual classes rather than fixed colours, so it survives the (currently
      unwired) theme switch.

## Data and state

- [ ] All I/O goes through a service in `main/app/service/`; no `HttpClient` or socket in a
      component.
- [ ] Service methods are typed, `catchError`-terminated, and re-throw via `throwError(() => error)`.
- [ ] New URLs added to all three `environments/*.ts`.
- [ ] Grids update via `applyTransaction`, not array reassignment; rows keyed by `getRowId`.
- [ ] Every `tradeFeed.subscribe(...)` teardown is stored and called on input change **and** in
      `ngOnDestroy`.
- [ ] Nothing displayed is fabricated: every number on screen comes from an existing endpoint
      (no price/notional/P&L — the API has none).

## Selectors and a11y

- [ ] Every new interactive control and grid has a stable `id`, matching the existing naming.
- [ ] Labels or `aria-label` on all inputs; explicit `type` on all buttons.
- [ ] State is never colour-only.

## Verification

- [ ] Ran the stack (`docker compose up`, UI on `:8080` — or the Angular dev server on `:18093`
      against running services) and exercised the change in a browser.
- [ ] Screenshot in the PR description. For a design-driven change, mock and implementation side
      by side.
- [ ] Live behaviour checked where relevant: create a trade, watch the blotter row appear and its
      state flash through `New → Processing → Settled`.
