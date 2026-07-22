# Header / top navigation (React migration 4/10)

Port of the Angular `app-header` component (`web-front-end/angular/main/app/header/`)
plus the theme-switch action wired in `app.component.html`.

## Exports

- `Header` (named) / `default` — the navbar + nav tabs component.
- `HeaderProps` — `{ onSwitchTheme?: () => void }`.

```tsx
import { Header } from './components/Header';
```

## INTEGRATION NOTE

`Header` renders a fixed FINOS/TraderX navbar and two `NavLink` tabs:
`/trade` and `/account`. It uses `react-router-dom`'s `NavLink`, so it **must be
rendered inside a router** (e.g. `<BrowserRouter>`), which the integration pass
should set up in `App.tsx` / `index.tsx`.

Suggested mount in `App.tsx`:

```tsx
<BrowserRouter>
  <Header onSwitchTheme={themeService.switchTheme} />
  <Routes>
    <Route path="/trade" element={<TradePage />} />
    <Route path="/account" element={<AccountPage />} />
  </Routes>
</BrowserRouter>
```

`onSwitchTheme` is optional; when omitted the "Switch theme" button is hidden.
Wire it to whatever the theme piece exposes — this component intentionally does
**not** import the theme logic.

## Assets

Logo images were copied from `web-front-end/angular/main/assets/img/` into
`web-front-end/react/public/`:
- `traderx-apple-touch-icon.png`
- `FINOS_Icon_White.png`

They are referenced via `process.env.PUBLIC_URL`.

## Dependency

Adds `react-router-dom` to `react/package.json` (used for `NavLink`).
