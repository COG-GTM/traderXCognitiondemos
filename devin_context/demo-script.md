# Demo: "follow our frontend conventions" + a design mock

**Story:** the team's frontend standards and design system live in the repo as
[`devin_context/`](README.md). A designer drops a PNG mock in `devin_context/mocks/`. Devin reads
both and ships the change — conventions applied, tests written, screenshot attached — with no
hand-holding in the prompt.

**Repo:** `COG-GTM/traderXCognitiondemos` · **Surface:** `web-front-end/angular` (Angular 18)

## Setup (30 seconds on screen)

1. Open [`devin_context/frontend-conventions.md`](frontend-conventions.md) — structure, `@Input()`
   account pattern, trade-feed subscribe/unsubscribe, SCSS rules, spec requirements.
2. Open [`devin_context/design-system.md`](design-system.md) — colour tokens, type scale, the
   "metric card" spec (4px semantic accent bar, uppercase label → value → caption).
3. Open [`devin_context/mocks/trade-page-positions-summary.png`](mocks/trade-page-positions-summary.png)
   — the ask, as a picture.

## Prompts

**1 — Ask Devin (discover)**
> Read `devin_context/` in COG-GTM/traderXCognitiondemos and summarise the frontend conventions and
> design tokens I'd have to follow. Which existing Angular component is the closest template to copy?

**2 — Ask Devin (scope the mock)**
> Look at `devin_context/mocks/trade-page-positions-summary.png` and tell me exactly what is new
> versus today's Trade page. List the files you'd touch and where the numbers come from.

**3 — Devin session (execute)**
> Implement the design in `devin_context/mocks/trade-page-positions-summary.png` in the Angular front
> end, following `devin_context/frontend-conventions.md` and `devin_context/design-system.md` exactly.
> Run the unit tests, and attach a screenshot of the running UI to the PR.

## What to point out in the result

- The mock was a **PNG**, not a ticket: labels, ordering, green/red semantics and placement above the
  blotters all came from the image.
- Conventions were **applied, not restated**: NgModule declaration, `ngOnChanges` account pattern,
  trade-feed unsubscribe on destroy, tokens from the design system, no new dependencies.
- A `.spec.ts` shipped with the component, using the repo's existing mock services.
- The PR carries a **screenshot of the running app** next to the mock — reviewers diff pixels, not prose.

## Reset

Revert the PR (or delete the `position-summary` component folder and its two lines in
`trade.module.ts` / `trade.component.html`) to re-run the demo from a clean slate.
