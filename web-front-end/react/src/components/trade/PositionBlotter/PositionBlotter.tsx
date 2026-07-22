import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import {
  ColDef,
  GetRowIdParams,
  GridApi,
  GridReadyEvent,
} from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { Account, Position } from '../../../models';
import { positionService } from '../../../services/positionService';
import { tradeFeed } from '../../../services/tradeFeed';

// SECTION 6 — Position blotter (ag-grid + live position feed).
export interface PositionBlotterProps {
  account?: Account;
}

const columnDefs: ColDef[] = [
  {
    field: 'security',
    headerName: 'SECURITY',
  },
  {
    headerName: 'QUANTITY',
    field: 'quantity',
    enableCellChangeFlash: true,
  },
];

export const PositionBlotter = ({ account }: PositionBlotterProps) => {
  const [positions, setPositions] = useState<Position[]>([]);
  const gridApiRef = useRef<GridApi | null>(null);
  const pendingRef = useRef(true);
  const pendingBufferRef = useRef<Position[]>([]);

  const apply = useCallback((data: Position) => {
    const api = gridApiRef.current;
    if (!api) {
      return;
    }
    const row = api.getRowNode(`Position-${data.security}`);
    if (row) {
      api.applyTransaction({
        update: [Object.assign(row.data, { quantity: data.quantity })],
      });
    } else {
      api.applyTransaction({
        add: [
          {
            accountid: data.accountid,
            quantity: data.quantity,
            security: data.security,
            updated: data.updated,
          },
        ],
        addIndex: 0,
      });
    }
  }, []);

  useEffect(() => {
    if (!account) {
      return;
    }
    const accountId = account.id;
    pendingRef.current = true;
    pendingBufferRef.current = [];

    positionService
      .getPositions(accountId)
      .then((fetched: Position[]) => {
        setPositions(fetched);
        pendingBufferRef.current.forEach((position) => apply(position));
        pendingBufferRef.current = [];
        pendingRef.current = false;
      })
      .catch(() => {
        pendingRef.current = false;
      });

    const unsubscribe = tradeFeed.subscribe(
      `/accounts/${accountId}/positions`,
      (data: Position) => {
        if (pendingRef.current) {
          pendingBufferRef.current.push(data);
        } else {
          apply(data);
        }
      }
    );

    return () => {
      unsubscribe();
    };
  }, [account, apply]);

  const onGridReady = useCallback((params: GridReadyEvent) => {
    gridApiRef.current = params.api;
  }, []);

  const getRowId = useCallback(
    (params: GetRowIdParams<Position>) => `Position-${params.data.security}`,
    []
  );

  return (
    <>
      <h5>Positions</h5>
      <div
        className="ag-theme-alpine"
        style={{ width: '100%', height: '350px' }}
      >
        <AgGridReact
          columnDefs={columnDefs}
          rowData={positions}
          getRowId={getRowId}
          onGridReady={onGridReady}
        />
      </div>
    </>
  );
};
