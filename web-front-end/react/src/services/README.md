# API service layer (React migration 2/10)

Plain async `fetch`-based ports of the Angular `@Injectable` services, plus a
socket.io trade-feed helper/hook. All functions return Promises (not RxJS
Observables) and log + rethrow on error.

## Modules

- `accountService.ts` — `getAccounts()`, `addAccount(account)`, `addAccountUser(accountUser)`, `getAccountUsers()` (account service `:18088`).
- `positionService.ts` — `getTrades(accountId)`, `getPositions(accountId)` (position service `:18090`).
- `symbolService.ts` — `getStocks()` (reference data `:18085`), `createTicket(ticket)` (`POST` trade service `:18092` `/trade/`).
- `userService.ts` — `getUsers(searchText)` → maps `{ people }` to `User[]` (people service `:18089`).
- `tradeFeed.ts` — `subscribe(topic, cb)` returning an unsubscribe fn (ignores `from === 'System'`), plus a `useTradeFeed(topic, cb)` React hook. socket.io feed `:18086`.
- `config.ts` — `ServiceUrls` (base URLs from `window.location.hostname`) and the `httpJson` fetch wrapper.
- `types.ts` — local domain types (`Account`, `User`, `AccountUser`, `Trade`, `Position`, `TradeTicket`, `Stock`, `Side`, `State`).

## INTEGRATION NOTE

Import from `src/services` (barrel `index.ts`):

```ts
import { getAccounts, createTicket, useTradeFeed, Trade } from './services';
```

- Nothing needs to be mounted in `App.tsx`/routing — this is a stateless data layer.
  Components/hooks call these functions directly (e.g. in `useEffect`).
- `types.ts` is self-contained and duplicates the Angular models; during integration,
  de-dupe against any shared `types` module other pieces introduce.
- Base URLs are defined locally in `config.ts` and follow the Angular environment
  (source of truth), notably the people service on `:18089` — the pre-existing
  `src/env.ts` lists `:18095`, which is incorrect. Reconcile `env.ts` during integration.
