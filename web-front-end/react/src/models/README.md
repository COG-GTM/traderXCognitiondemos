# Shared domain models

Pure TypeScript type declarations ported from the Angular app
(`web-front-end/angular/main/app/model/*`). No runtime logic.

## Exports (`src/models/index.ts` re-exports all)

- `trade.ts` — `Trade`, `Position`, `TradeTicket` interfaces; `Side`, `State` enums.
- `account.ts` — `Account` interface.
- `user.ts` — `User`, `AccountUser` interfaces.
- `symbol.ts` — `Symbol`, `Stock` interfaces.

## INTEGRATION NOTE

Nothing to mount in `App.tsx` / routing — these are types only.
Other pieces should import shared domain types from here, e.g.:

```ts
import { Trade, Side, State, Position, Account, User } from '../models';
```

During integration, replace any locally-duplicated domain types in other
pieces' folders with imports from `src/models` to de-duplicate.
