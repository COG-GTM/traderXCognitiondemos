import React from 'react';
import { Account } from '../../../models';

// SECTION 6 — Position blotter (ag-grid + live position feed).
export interface PositionBlotterProps {
  account?: Account;
}

// Placeholder: replace with an ag-grid (SECURITY, QUANTITY columns with quantity
// cell flashing, row id `Position-<security>`) hydrated from
// positionService.getPositions and live-updated via tradeFeed.subscribe(
// `/accounts/<id>/positions`), buffering updates that arrive before hydration.
export const PositionBlotter = (_props: PositionBlotterProps) => {
  return <div data-testid="position-blotter-placeholder" />;
};
