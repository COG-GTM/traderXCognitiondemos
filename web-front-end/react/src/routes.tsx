import React from 'react';
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import AppShell from './AppShell';
import { AccountPage, NotFoundPage, TradePage } from './pages';

/**
 * Client-side routing mirroring the Angular `routing.ts`:
 *
 *   { path: 'trade',   component: TradeComponent }
 *   { path: 'account', component: AccountComponent }
 *   { path: '',        redirectTo: '/trade', pathMatch: 'full' }
 *   { path: '**',      component: PageNotFoundComponent }
 *
 * All routes are nested under `AppShell`, which provides the Header slot and
 * the <Outlet/> where the routed page renders.
 */

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/trade" replace /> },
      { path: 'trade', element: <TradePage /> },
      { path: 'account', element: <AccountPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

/**
 * INTEGRATION NOTE: `index.tsx` should render this `AppRouter` instead of the
 * current single `<App/>`, e.g.
 *
 *   import { AppRouter } from './routes';
 *   root.render(<React.StrictMode><AppRouter /></React.StrictMode>);
 */
export const AppRouter: React.FC = () => <RouterProvider router={router} />;

export default AppRouter;
