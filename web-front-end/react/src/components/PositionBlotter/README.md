# Position Blotter (React)

Port of the Angular `position-blotter.component` (`web-front-end/angular/main/app/trade/position-blotter/`).

Shows a live ag-grid of positions for a selected account:
- Hydrates via `GET /positions/{accountId}` (position service `:18090`).
- Subscribes to trade-feed topic `/accounts/{id}/positions` (`:18086`) for live updates,
  ignoring messages `from === 'System'`.
- Columns: `SECURITY` (`security`), `QUANTITY` (`quantity`, with cell-change flash).
- Rows keyed by `security`; uses `applyTransaction` add/update.
- Replicates the pending-buffer logic: feed messages that arrive before hydration
  completes are queued and flushed once positions are loaded.

## INTEGRATION NOTE

Exports (from `src/components/PositionBlotter`):
- `PositionBlotter` (named) and `default` — the React component.
- `Position`, `Account` — local types.

Mount in `App.tsx` / routing by passing the currently selected account:

```tsx
import { PositionBlotter } from './components/PositionBlotter';

// either a full account object or just an id
<PositionBlotter account={selectedAccount} />
// or
<PositionBlotter accountId={selectedId} />
```

The component fills its parent, so give it a sized container (e.g. a flex/grid cell
or `style={{ height: '80vh' }}`). It manages its own fetch + socket subscription and
cleans up on unmount / account change.

### Self-containment notes
- Types are defined locally in `types.ts` (no cross-piece imports).
- Trade-feed socket logic is local in `tradeFeed.ts`, following the `src/socket.ts`
  pattern. It creates its own socket.io connection to `:18086` so it does not collide
  with other pieces sharing the singleton in `src/socket.ts`.
- Service base URLs are computed from `window.location.hostname` like `src/env.ts`.
