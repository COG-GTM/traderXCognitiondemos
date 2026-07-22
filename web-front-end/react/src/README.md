# React migration (5/10): App shell + client-side routing

This piece ports the Angular app shell (`app.component.html`) and routing
(`routing.ts`) to React.

## Files

- `AppShell.tsx` — top-level layout. Renders a **placeholder** Header slot plus
  react-router's `<Outlet/>`, mirroring Angular's
  `<app-header></app-header>` + `<router-outlet></router-outlet>`.
- `routes.tsx` — `react-router-dom` `createBrowserRouter` + `RouterProvider`
  config. Exports `router`, and `AppRouter` (default export) that renders
  `<RouterProvider/>`.
- `pages/index.tsx` — local placeholder page components: `TradePage`,
  `AccountPage`, `NotFoundPage`.

## Routes (mirrors Angular `routing.ts`)

| Angular                                   | React (`routes.tsx`)                     |
| ----------------------------------------- | ---------------------------------------- |
| `{ path: 'trade', TradeComponent }`       | `/trade` → `<TradePage/>`                |
| `{ path: 'account', AccountComponent }`   | `/account` → `<AccountPage/>`            |
| `{ path: '', redirectTo: '/trade' }`      | index → `<Navigate to="/trade" replace/>`|
| `{ path: '**', PageNotFoundComponent }`   | `*` → `<NotFoundPage/>`                  |

All routes are nested under `<AppShell/>`.

## INTEGRATION NOTE

- `index.tsx` should render this piece instead of the current single `<App/>`:

  ```tsx
  import { AppRouter } from './routes';
  root.render(
    <React.StrictMode>
      <AppRouter />
    </React.StrictMode>
  );
  ```

- The placeholders are intentionally self-contained (no imports from other
  pieces). On integration:
  - Replace `HeaderPlaceholder` in `AppShell.tsx` with the real **Header**
    component (wire its theme-switch callback, mirroring Angular's
    `(switchTheme)` output).
  - Replace the bodies of `TradePage` / `AccountPage` in `pages/index.tsx` with
    the real **Trade** and **Account** feature components. Keep the exported
    names stable so `routes.tsx` needs no changes.

## Dependency added

- `react-router-dom` (`^6.26.2`) added to `package.json`.
