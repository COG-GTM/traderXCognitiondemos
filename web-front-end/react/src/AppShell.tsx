import React from 'react';
import { Outlet } from 'react-router-dom';

/**
 * AppShell — top-level layout mirroring the Angular `app.component.html`:
 *
 *   <app-header></app-header>
 *   <router-outlet></router-outlet>
 *
 * It renders a placeholder Header slot followed by the routed page via
 * react-router's <Outlet/>.
 *
 * INTEGRATION NOTE: `HeaderPlaceholder` below is intentionally a local,
 * self-contained stub. Once the real Header piece is merged, replace the
 * placeholder with the actual `<Header/>` component (and wire up its
 * theme-switch callback, which mirrors Angular's `(switchTheme)` output).
 */

const HeaderPlaceholder: React.FC = () => (
  <header className="app-header-placeholder">
    <nav>
      <strong>FINOS | TraderX Sample Application</strong>
    </nav>
    <p>Header placeholder — replace with the real Header component on integration.</p>
  </header>
);

export const AppShell: React.FC = () => (
  <div className="App">
    <HeaderPlaceholder />
    <main>
      <Outlet />
    </main>
  </div>
);

export default AppShell;
