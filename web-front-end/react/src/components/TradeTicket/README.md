# Trade Ticket (React migration piece 8/10)

Port of the Angular `app-trade-ticket` (new-trade entry form) plus the
open/create-ticket flow from `trade.component.ts`.

Source of truth:
- `web-front-end/angular/main/app/trade/trade-ticket/trade-ticket.component.{ts,html}`
- `web-front-end/angular/main/app/trade/trade.component.ts` (`createTradeTicket`, `openTicket`)

## What this exports

```ts
import { TradeTicket } from './components/TradeTicket';
// or: import TradeTicket from './components/TradeTicket';
```

- `TradeTicket` — the form component (default + named export).
- `TradeTicketProps` — props type.
- `Side`, `Stock`, `TradeTicketModel` — local types.
- `fetchStocks`, `createTrade`, `ServiceUrls` — local data helpers.

## Behavior

- Loads securities from reference-data `GET /stocks` (`:18085`) when visible.
- Fields: security select (shows `companyName (ticker)`, value = `ticker`),
  quantity number input, Buy/Sell toggle (defaults to `Buy`).
- **Create** validates that security and quantity are set (mirrors Angular's
  `onCreate`), then `POST`s `{ side, quantity, security, accountId }` to the
  trade service `POST /trade/` (`:18092`) and shows success/error feedback.
- **Close** calls `onClose`.

## Props

| prop | type | notes |
|------|------|-------|
| `accountId` | `number` | required — account the trade is booked against |
| `accountName` | `string?` | optional read-only account label |
| `open` | `boolean?` | if provided, renders inside a MUI `Modal`; if omitted, renders inline |
| `onClose` | `() => void` | called on Close/backdrop dismiss |
| `onCreated` | `(ticket) => void` | called after a successful create |

## INTEGRATION NOTE

`App.tsx` / routing should mount `TradeTicket` wherever the "Create New Trade"
action lives (it can replace the modal inside `src/ActionButtons/CreateTradeButton.tsx`).

Modal usage (matches the Angular open/close flow):

```tsx
const [open, setOpen] = useState(false);
<Button onClick={() => setOpen(true)}>Create New Trade</Button>
<TradeTicket
  accountId={selectedAccountId}
  accountName={selectedAccountName}
  open={open}
  onClose={() => setOpen(false)}
  onCreated={() => setOpen(false)}
/>
```

Inline usage (no modal): omit `open`.

Service URLs are computed locally in `api.ts` from `window.location.hostname`
following the `src/env.ts` pattern; de-dupe against a shared env module during
integration. Note: reference-data is `:18085`, trade service `:18092`.
