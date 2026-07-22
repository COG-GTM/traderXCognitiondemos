import React from 'react';
import { ICellRendererParams } from 'ag-grid-community';

// SECTION 10 — ag-grid "Update" button cell renderer.
// Used by the account list grid. `clicked` receives the row's data.
export type ButtonCellRendererProps = ICellRendererParams & {
  clicked: (data: any) => void;
};

// Placeholder: replace with a small Bootstrap button that calls
// `props.clicked(props.data)` when pressed.
export const ButtonCellRenderer = (_props: ButtonCellRendererProps) => {
  return <span data-testid="button-cell-renderer-placeholder" />;
};
