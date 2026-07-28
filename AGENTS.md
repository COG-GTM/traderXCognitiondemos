# TraderX — agent notes

FINOS reference trading application: a deliberately polyglot, distributed sample. Java/Spring,
Node/NestJS, Node/socket.io, .NET, H2, and an Angular UI (plus a secondary React UI).

## Frontend work: read `devin_context/frontend/` first

Those docs are **normative** for `web-front-end/angular` — dependency policy and file layout
(`conventions.md`), the Bootstrap 5 / AG Grid design system (`design-system.md`), container and
blotter skeletons (`component-patterns.md`), services, RxJS and the trade feed (`data-and-state.md`),
spec shape and the `id`-selector contract (`testing-and-selectors.md`), and the pre-PR gate
(`review-checklist.md`). Mocks with specs live in `devin_context/design/`.

Two things that catch people out: **no new frontend dependencies**, and `web-front-end/angular` is
the canonical UI — `web-front-end/react` is a hack-day contribution, don't port patterns out of it
or change it unless the task says so.

## Running it

```bash
docker compose up          # everything, UI on http://localhost:8080
```

Services can also be run by hand; the root `README.md` has the port table
(`18082`–`18094`) and the required start order (database → reference-data → trade-feed →
people-service → account-service → position-service → trade-processor → trade-service →
web-front-end).

## Build and test commands

| Area | Commands |
| --- | --- |
| Angular UI (`web-front-end/angular`) | `npm install`; `npm start` (:18093); `npm run build` (prod AOT — the gate); `npm run test:ci` (the other gate). `npm run lint` is broken — it points at the tslint builder removed in Angular 12 |
| Java services (`account-service`, `position-service`, `trade-service`, `trade-processor`, `database`) | `./gradlew :<service>:build`, `./gradlew :<service>:test`, `./gradlew :<service>:bootRun` |
| Node services (`reference-data`, `trade-feed`) | `npm install`, `npm run build`, `npm test`, `npm start` in the service directory |
| .NET (`people-service`) | `dotnet build`, `dotnet test`, `dotnet run` |

## Conventions

- Service ports and inter-service URLs are environment-driven; the Angular UI resolves them in
  `web-front-end/angular/main/environments/*.ts`. Add new URLs to all three files.
- Trade lifecycle states are `New → Processing → Pending → Settled`, published on the socket.io
  trade feed under `/accounts/{id}/trades` and `/accounts/{id}/positions`.
- No endpoint returns a price, notional or P&L. Don't build UI that implies one.
- Contribution process (issue first, PR, FINOS CLA) is in `CONTRIBUTING.md`.
