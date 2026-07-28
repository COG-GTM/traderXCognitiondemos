# devin_context/

Curated context for AI coding agents (Devin) working on TraderX. Anything in this folder is
intended to be read *before* writing code — it encodes the conventions a human reviewer would
otherwise have to repeat in every PR review.

| File | What it is |
| --- | --- |
| [`frontend-conventions.md`](frontend-conventions.md) | How we build Angular UI in `web-front-end/angular` — structure, naming, state, tests |
| [`design-system.md`](design-system.md) | Visual language: colour tokens, type scale, spacing, card/blotter patterns |
| [`mocks/`](mocks/) | Design mocks (PNG). Each mock has a matching entry in [`mocks/README.md`](mocks/README.md) |
| [`demo-script.md`](demo-script.md) | Walkthrough for demoing "here are our conventions + a mock, go build it" |

## How to use it

Point Devin at this folder and the mock, and let it derive the implementation:

> Read `devin_context/frontend-conventions.md` and `devin_context/design-system.md`, then
> implement the design in `devin_context/mocks/trade-page-positions-summary.png` in the Angular
> front end. Follow these frontend conventions exactly — component layout, SCSS tokens, and
> unit-test expectations. Attach a screenshot of the running UI to the PR.

Rules of thumb for the agent:

1. **Conventions beat cleverness.** If this folder and your instinct disagree, this folder wins.
2. **Mocks are specs.** Match labels, ordering, colour semantics and placement in the mock. If the
   mock is ambiguous, follow `design-system.md`; if that is also silent, say so in the PR.
3. **No new dependencies** to satisfy a mock. Bootstrap 5 + the existing SCSS is enough.
4. **Every visual change ships with a screenshot** of the running app in the PR description.
