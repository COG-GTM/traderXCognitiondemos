import { useEffect, useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import type { ColDef, GetRowIdParams, GridApi, GridReadyEvent } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import type { Account } from '../../models/account.model';
import type { Position } from '../../models/trade.model';
import { getPositions } from '../../services/position.service';
import { subscribe } from '../../services/trade-feed.service';

const columnDefs: ColDef<Position>[] = [
  {
    field: 'security',
    headerName: 'SECURITY'
  },
  {
    headerName: 'QUANTITY',
    field: 'quantity',
    enableCellChangeFlash: true
  }
];

function getRowId(params: GetRowIdParams<Position>): string {
  return `Position-${params.data.security}`;
}

export interface PositionBlotterProps {
  account?: Account;
}

export function PositionBlotter({ account }: PositionBlotterProps) {
  const [positions, setPositions] = useState<Position[]>([]);
  const gridApiRef = useRef<GridApi<Position> | null>(null);
  const isPendingRef = useRef(true);
  const pendingPositionsRef = useRef<Position[]>([]);

  const update = (data: Position) => {
    const gridApi = gridApiRef.current;
    if (!gridApi) {
      return;
    }
    const row = gridApi.getRowNode(`Position-${data.security}`);
    if (row && row.data) {
      gridApi.applyTransaction({
        update: [Object.assign(row.data, { quantity: data.quantity })]
      });
    } else {
      gridApi.applyTransaction({
        add: [{
          accountid: data.accountid,
          quantity: data.quantity,
          security: data.security,
          updated: data.updated
        }],
        addIndex: 0
      });
    }
  };

  const updatePosition = (data: Position) => {
    if (isPendingRef.current) {
      pendingPositionsRef.current.push(data);
    } else {
      update(data);
    }
  };

  useEffect(() => {
    if (!account) {
      return;
    }
    const accountId = account.id;
    isPendingRef.current = true;
    let cancelled = false;

    getPositions(accountId)
      .then((data) => {
        if (cancelled) {
          return;
        }
        setPositions(data);
        pendingPositionsRef.current.forEach((position) => update(position));
        pendingPositionsRef.current = [];
        isPendingRef.current = false;
      })
      .catch(() => {
        if (!cancelled) {
          isPendingRef.current = false;
        }
      });

    const unsubscribe = subscribe(`/accounts/${accountId}/positions`, (data: Position) => {
      updatePosition(data);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account?.id]);

  const onGridReady = (params: GridReadyEvent<Position>) => {
    gridApiRef.current = params.api;
  };

  return (
    <div>
      <h5>Positions</h5>
      <div className="ag-theme-alpine" style={{ width: '100%', height: 350 }}>
        <AgGridReact<Position>
          columnDefs={columnDefs}
          rowData={positions}
          onGridReady={onGridReady}
          getRowId={getRowId}
        />
      </div>
    </div>
  );
}

export default PositionBlotter;
