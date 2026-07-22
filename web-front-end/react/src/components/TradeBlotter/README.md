# Trade Blotter (React migration 6/10)

Live trades grid for a single account, ported from the Angular
`trade-blotter.component` (`web-front-end/angular/main/app/trade/trade-blotter/`).

## What it does
- Hydrates via `GET /trades/{accountId}` on the position service (`:18090`).
- Subscribes to the trade feed topic `/accounts/{id}/trades` (socket.io, `:18086`),
  ignoring messages where `from === 'System'`.
- Applies `add`/`update` grid transactions keyed by trade id (`Trade-${id}`).
- Buffers feed updates that arrive before hydration finishes, then replays them.
- Columns: SECURITY, QUANTITY, SIDE, STATE (flashes on change), UPDATED.

## Exports
- `TradeBlotter` (named + default) — `React.FC<TradeBlotterProps>`.
- `TradeBlotterProps` — `{ accountId?: number }`.
- `Trade`, `Side`, `State`, `FeedMessage` — local types.

## INTEGRATION NOTE
Mount inside the trade area, passing the currently selected account id:

```tsx
import { TradeBlotter } from './components/TradeBlotter';

<TradeBlotter accountId={selectedAccountId} />
```

The component manages its own data fetching and socket lifecycle (connects on
mount / when `accountId` changes, unsubscribes + disconnects on cleanup), so
`App.tsx` only needs to supply `accountId`.

Self-contained per the parallel-migration rules: service URLs, socket setup,
and the `Trade`/`Side`/`State` types are duplicated locally in this folder
(`types.ts`) rather than imported from other pieces. De-dupe against
`src/env.ts` / `src/socket.ts` during the integration pass.
