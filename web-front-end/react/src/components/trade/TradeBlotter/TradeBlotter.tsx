import React from 'react';
import { Account } from '../../../models';

// SECTION 5 — Trade blotter (ag-grid + live trade feed).
export interface TradeBlotterProps {
  account?: Account;
}

// Placeholder: replace with an ag-grid (SECURITY, QUANTITY, SIDE, STATE columns,
// row id `Trade-<id>`, state cell flashing) hydrated from
// positionService.getTrades and live-updated via tradeFeed.subscribe(
// `/accounts/<id>/trades`), buffering updates that arrive before hydration.
export const TradeBlotter = (_props: TradeBlotterProps) => {
  return <div data-testid="trade-blotter-placeholder" />;
};
