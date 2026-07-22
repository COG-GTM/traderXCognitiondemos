# TraderX Angular → React Migration

This app is being migrated from the Angular front-end (`web-front-end/angular`)
to React. This document defines the shared architecture and the 10 independent
sections the migration is split into.

## Shared foundation (this branch)

The base branch (`devin/react-migration-base`) provides the contracts every
section builds on. **Do not change these contracts without coordinating** — the
sections are being implemented in parallel and rely on them:

- `src/models/` — domain types (`Account`, `Trade`, `Position`, `Stock`,
  `User`, `AccountUser`, `TradeTicket`, `Side`, `State`).
- `src/config/environment.ts` — backend service URLs (ports mirror the Angular
  `environment.ts`).
- `src/services/` — promise-based service layer replacing the Angular
  `HttpClient` services: `accountService`, `positionService`, `symbolService`,
  `userService`, plus the shared `http` helpers.
- `src/services/tradeFeed.ts` — socket.io singleton (`subscribe(topic, cb)`
  returns an unsubscribe fn), replacing `TradeFeedService`.
- `src/context/ThemeContext.tsx` — `ThemeProvider` + `useTheme()` for
  light/dark theme switching, replacing `ThemeService`.
- `src/App.tsx` + `src/index.tsx` — react-router routes (`/trade`, `/account`,
  `*` → 404) and app bootstrap (Bootstrap CSS, `ThemeProvider`, `BrowserRouter`).

Each section replaces a **placeholder** component. Placeholders keep the app
compiling; every section should build + typecheck cleanly and only touch files
inside its own directory (plus adding tests).

## The 10 sections

| # | Section | Directory | Replaces (Angular) |
|---|---------|-----------|--------------------|
| 1 | Header & navigation | `components/Header/` | `header/`, `app.component` |
| 2 | Reusable dropdown | `components/Dropdown/` | `dropdown/` |
| 3 | Trade page container | `components/trade/TradePage/` | `trade/trade.component` |
| 4 | Trade ticket form | `components/trade/TradeTicket/` | `trade/trade-ticket/` |
| 5 | Trade blotter (ag-grid + feed) | `components/trade/TradeBlotter/` | `trade/trade-blotter/` |
| 6 | Position blotter (ag-grid + feed) | `components/trade/PositionBlotter/` | `trade/position-blotter/` |
| 7 | Accounts page container | `components/accounts/AccountsPage/` | `accounts/account.component` |
| 8 | Edit account form | `components/accounts/EditAccount/` | `accounts/edit/` |
| 9 | Assign user to account | `components/accounts/AssignUser/` | `accounts/user/` |
| 10 | Grid button renderer + 404 | `components/shared/` | `accounts/button-renderer`, `page-not-found` |

## Development

```bash
npm install
npm start   # dev server on $WEB_SERVICE_REACT_PORT (default 18094)
npm run build
npm test
```
