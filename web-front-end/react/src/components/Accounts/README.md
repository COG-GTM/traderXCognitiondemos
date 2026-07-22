# Accounts page (React migration 9/10)

Ports the Angular Accounts management page to React:

- Angular sources: `web-front-end/angular/main/app/accounts/account.component.{ts,html}`,
  `accounts/edit/edit.component.{ts,html}`, `accounts/button-renderer.component.ts`.

## Files

- `AccountsPage.tsx` — composes `EditAccount` + `AccountList`. Owns the shared
  state (`accountToBeUpdate`, `selectedAccount`, refresh trigger).
- `AccountList.tsx` — the "Account List" ag-grid (columns `id`, `displayName`,
  and a per-row **Update** button via a React cell renderer) plus the
  "Users List" ag-grid for the selected account.
- `EditAccount.tsx` — the add/update account form (`POST /account/`) with a
  success/error alert that auto-dismisses after 2s.
- `api.ts` — local data fetching against the account service (`:18088`):
  `GET /account/`, `GET /accountuser/`, `POST /account/`. URL is computed from
  `window.location.hostname` like `src/env.ts`.
- `types.ts` — local `Account` / `AccountUser` types.

## Behavior

- Account List is fed by `GET /account/`. Selecting a row (or a successful
  add/update) selects that account and refreshes both grids.
- The row **Update** button prefills the edit form with that account
  (Angular's `accountToBeUpdate`); it does not by itself change the selection.
- Users List is fed by `GET /accountuser/`, filtered by the selected account's
  `id`, and its header shows the selected account's `displayName`.

## INTEGRATION NOTE

Mount `AccountsPage` at the accounts route in `src/App.tsx` / routing:

```tsx
import { AccountsPage } from './components/Accounts';

// ...
<AccountsPage />
```

The Angular page also renders `<app-assign-user>` between the edit form and the
Users List. That is **piece 10 (AssignUser)** and is intentionally NOT imported
here. `AccountsPage` accepts an optional `assignUserSlot?: ReactNode` prop;
during integration pass the AssignUser component through it, e.g.:

```tsx
<AccountsPage assignUserSlot={<AssignUser account={selectedAccount} accounts={accounts} />} />
```

(You may prefer to lift `selectedAccount` into `AccountsPage` or wire AssignUser
directly — the slot is just a non-importing placeholder to keep this piece
standalone.)

No changes were made to `App.tsx`, `index.tsx`, `index.css`, or routing files.
Only a self-contained set of new files under `src/components/Accounts/` was added.
