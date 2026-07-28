# `devin_context/` — frontend conventions for TraderX

The rules this repo's UI is actually built to. They are **normative**: an agent (or a new hire)
should be able to read this folder and produce a screen nobody can tell apart from the existing
ones. Everything here was reverse-engineered from the code in `web-front-end/angular`, not invented.

If code disagrees with these docs, the docs win — open a PR to change the doc, don't silently
diverge.

## Scope

TraderX ships two UIs. **`web-front-end/angular` is the canonical one** — it is what
`docker compose up` serves on `:8080`, it is the only one with account management, and it is what
these docs describe. `web-front-end/react` is a hack-day contribution kept for reference; don't
port patterns out of it, and don't change it unless the task names it.

## Read these

| Doc | What it settles |
| --- | --- |
| [`frontend/conventions.md`](frontend/conventions.md) | Stack rules, dependency policy, file layout, naming, code style |
| [`frontend/design-system.md`](frontend/design-system.md) | Bootstrap 5 as the design system: utilities, button semantics, AG Grid theme, when SCSS is allowed |
| [`frontend/component-patterns.md`](frontend/component-patterns.md) | Feature module, container/presentational split, blotter, modal ticket, dropdown, cell renderer |
| [`frontend/data-and-state.md`](frontend/data-and-state.md) | Services, `HttpClient`, RxJS, the socket.io trade feed, teardown |
| [`frontend/testing-and-selectors.md`](frontend/testing-and-selectors.md) | Karma/Jasmine spec shape, mock services, the `id` selector contract |
| [`frontend/review-checklist.md`](frontend/review-checklist.md) | The gate to pass before opening a PR |

## Golden files — mirror these, don't invent

| Pattern | File |
| --- | --- |
| Feature page (container) | `web-front-end/angular/main/app/trade/trade.component.ts` |
| AG Grid blotter + live feed | `web-front-end/angular/main/app/trade/trade-blotter/trade-blotter.component.ts` |
| Presentational form child | `web-front-end/angular/main/app/trade/trade-ticket/trade-ticket.component.ts` |
| Reactive list page | `web-front-end/angular/main/app/accounts/account.component.ts` |
| Feature module wiring | `web-front-end/angular/main/app/trade/trade.module.ts` |
| REST service | `web-front-end/angular/main/app/service/account.service.ts` |
| Streaming service | `web-front-end/angular/main/app/service/trade-feed.service.ts` |
| Reusable presentational component | `web-front-end/angular/main/app/dropdown/dropdown.component.ts` |
| AG Grid cell renderer | `web-front-end/angular/main/app/accounts/button-renderer.component.ts` |
| Spec + mocks | `web-front-end/angular/main/app/trade/trade-blotter/trade-blotter.component.spec.ts`, `main/app/test-utils/mocks.service.ts` |
| Models | `web-front-end/angular/main/app/model/*.model.ts` |
| Global styles | `web-front-end/angular/main/styles.scss` |

## Design

[`design/`](design/) holds mocks with a short spec beside each, written in the same vocabulary as
the docs above (`btn-info`, `ag-theme-alpine`, `ColDef`, `async` pipe) so "match the design" and
"follow our conventions" are one instruction:

- [`design/blotter-summary.png`](design/blotter-summary.png) +
  [`design/blotter-summary.md`](design/blotter-summary.md) — target state for the Trade page.

## Prompts

[`prompts/`](prompts/) holds copy-paste prompts that point Devin at this folder:

- [`prompts/frontend-feature.md`](prompts/frontend-feature.md) — build a new feature under these conventions.
- [`prompts/frontend-audit.md`](prompts/frontend-audit.md) — read-only audit of the existing UI against them.
- [`prompts/frontend-from-mock.md`](prompts/frontend-from-mock.md) — build a screen from a mock in `design/`.

[`DEMO.md`](DEMO.md) is the runbook for demoing this folder.

## Related

- [`../AGENTS.md`](../AGENTS.md) — repo-wide commands, ports, and service map.
- [`../README.md`](../README.md) — architecture, `docker compose up`, port table.
