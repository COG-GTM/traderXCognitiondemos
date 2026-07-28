# Prompt: build a screen from a mock

Copy from the line below into a Devin session on this repo.

---

Before writing any code, read `devin_context/README.md` and every doc under
`devin_context/frontend/`. They are the normative frontend conventions for this repo. Then open the
mock image `devin_context/design/blotter-summary.png` and read the spec beside it,
`devin_context/design/blotter-summary.md`. Look at the image — the spec describes it, it doesn't
replace it.

[feature] Implement that design on the Trade page (`web-front-end/angular/main/app/trade/`): the
summary cards and the state/security filter above the existing blotters. Match the mock's layout,
spacing and typography using Bootstrap utilities only — no new CSS beyond what the conventions
allow, no new dependency.

The mock is a design artifact, not a contract with the backend: **anything it shows that the
existing services cannot supply must be left out and flagged in your PR description**, with the
reason. Do not invent, estimate, or hard-code a value to make a tile look full.

Keep `TradeComponent` the container, add presentational children with `@Input()`/`@Output()`, give
every new control a stable `id`, and add a spec per component using
`main/app/test-utils/mocks.service.ts`.

Before opening the PR run `npm --prefix web-front-end/angular run build` and
`npm --prefix web-front-end/angular run test:ci`, bring the stack up (`docker compose up`, UI on
`:8080`), and exercise it in a browser: switch accounts, apply each filter, create a trade and
watch the counts and the blotter update live. Put the mock and your screenshot **side by side** in
the PR description, and paste `devin_context/frontend/review-checklist.md` with every box ticked.
