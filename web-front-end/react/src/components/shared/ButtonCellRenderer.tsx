import React from 'react';
import { ICellRendererParams } from 'ag-grid-community';

// SECTION 10 — ag-grid "Update" button cell renderer.
// Used by the account list grid. `clicked` receives the row's data.
export type ButtonCellRendererProps = ICellRendererParams & {
  clicked: (data: any) => void;
};

export const ButtonCellRenderer = (props: ButtonCellRendererProps) => {
  const clickHandler = () => {
    props.clicked(props.data);
  };

  return (
    <button className="btn btn-sm btn-info" onClick={clickHandler}>
      Update
    </button>
  );
};
