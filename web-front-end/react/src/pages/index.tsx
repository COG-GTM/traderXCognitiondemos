import React from 'react';

/**
 * Placeholder page components for the React app shell.
 *
 * INTEGRATION NOTE: these placeholders mirror the Angular routed components
 * (`TradeComponent`, `AccountComponent`, `PageNotFoundComponent`). Once the
 * real pieces are merged, replace the bodies below with the actual feature
 * components (e.g. TradeBlotter / TradeTicket for the trade page, and the
 * accounts view for the account page). Keep the exported names stable so
 * `routes.tsx` does not need to change.
 */

export const TradePage: React.FC = () => (
  <div className="page page--trade">
    <h2>Trade</h2>
    <p>Trade page — mount TradeBlotter / TradeTicket here.</p>
  </div>
);

export const AccountPage: React.FC = () => (
  <div className="page page--account">
    <h2>Account</h2>
    <p>Account page — mount the Account view here.</p>
  </div>
);

// Mirrors Angular `PageNotFoundComponent` (template: `<h2>Page not found</h2>`).
export const NotFoundPage: React.FC = () => (
  <div className="page page--not-found">
    <h2>Page not found</h2>
  </div>
);
