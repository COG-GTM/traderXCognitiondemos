# AssignUser + Dropdown (React migration piece 10/10)

React ports of the Angular assign-user-to-account form
(`angular/main/app/accounts/user/assign-user.component.*`) and the generic
single-select dropdown (`angular/main/app/dropdown/dropdown.component.*`).

## Components

### `<AssignUser />` — `components/AssignUser/AssignUser.tsx`
A people typeahead + account selector. Typing more than 2 characters queries the
people service (debounced ~300ms); selecting a user and clicking **Add User**
POSTs `{ username: user.logonId, accountId }` to the account service and shows a
success/error alert that auto-dismisses after 2s.

Props:
- `account?: Account` — optionally preselected account.
- `accounts: Account[]` — accounts for the dropdown.
- `onUpdate?: (account: Account) => void` — fired after a successful add
  (Angular `update` output).

### `Dropdown<T>` — `components/Dropdown/Dropdown.tsx`
Generic reusable single-select dropdown mirroring the Angular API:
`items`, `itemKey`, `selectedItem`, `placeholder`, `onSelectedItemChange`
(was `selectedItemChange`), and optional `selectionComparator`. Only emits a
change when the selection differs from the current one.

## Data / services (self-contained)
`components/AssignUser/api.ts` computes base URLs from
`window.location.hostname` (like `src/env.ts`):
- people service **:18089** — `GET /People/GetMatchingPeople?SearchText=&Take=10`
  (Angular's source-of-truth port; the shared `env.ts` currently lists the wrong
  :18095).
- account service **:18088** — `POST /accountuser/`.

Types are defined locally (`AssignUser/types.ts`, `Dropdown/types.ts`) to keep
this piece independent.

## INTEGRATION NOTE
- Import from the folder barrels:
  `import { AssignUser } from './components/AssignUser';`
  `import { Dropdown } from './components/Dropdown';`
- `App.tsx` / routing should mount `<AssignUser accounts={accounts} account={selectedAccount} onUpdate={reloadAccount} />`
  on the accounts/admin view, passing the account list it already loads
  (e.g. via the existing `GetAccounts` hook) and re-fetching on `onUpdate`.
- `Dropdown<T>` is generic and reusable anywhere a single-select is needed.
- Uses `@mui/material` only (already a dependency); no new UI kit introduced.
- During integration, the local `Account`/`User`/`AccountUser` types and the
  service URLs here can be de-duped against the shared `env.ts` (after its
  people-service port is corrected to :18089).
