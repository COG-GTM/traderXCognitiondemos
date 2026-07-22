import { useEffect, useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import type { ColDef, GridApi, GridReadyEvent, GetRowIdParams } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import type { Account } from '../../models/account.model';
import type { Trade } from '../../models/trade.model';
import { getTrades } from '../../services/position.service';
import { subscribe } from '../../services/trade-feed.service';

export const columnDefs: ColDef[] = [
    {
        headerName: 'SECURITY',
        field: 'security'
    },
    {
        headerName: 'QUANTITY',
        field: 'quantity'
    },
    {
        headerName: 'SIDE',
        field: 'side'
    },
    {
        headerName: 'STATE',
        field: 'state',
        enableCellChangeFlash: true
    }
];

export function getRowId(params: GetRowIdParams<Trade>): string {
    return params.data.id;
}

interface TradeBlotterProps {
    account?: Account;
}

function TradeBlotter({ account }: TradeBlotterProps) {
    const [trades, setTrades] = useState<Trade[]>([]);
    const gridApiRef = useRef<GridApi | null>(null);
    const pendingTradesRef = useRef<Trade[]>([]);
    const isPendingRef = useRef(true);

    useEffect(() => {
        if (!account) {
            return;
        }
        let cancelled = false;
        isPendingRef.current = true;
        getTrades(account.id)
            .then((result) => {
                if (cancelled) {
                    return;
                }
                setTrades(result);
                pendingTradesRef.current.forEach((tradeUpdate) => update(tradeUpdate));
                pendingTradesRef.current = [];
                isPendingRef.current = false;
            })
            .catch((error) => console.error(error));
        const unsubscribe = subscribe(`/accounts/${account.id}/trades`, (data: Trade) => {
            if (isPendingRef.current) {
                pendingTradesRef.current.push(data);
            } else {
                update(data);
            }
        });
        return () => {
            cancelled = true;
            unsubscribe();
        };
    }, [account?.id]);

    const update = (data: Trade) => {
        const gridApi = gridApiRef.current;
        if (!gridApi) {
            return;
        }
        const row = gridApi.getRowNode(data.id);
        if (row) {
            gridApi.applyTransaction({
                update: [Object.assign(row.data, { state: data.state })]
            });
        } else {
            gridApi.applyTransaction({
                add: [{
                    accountid: data.accountid,
                    created: data.created,
                    id: data.id,
                    quantity: data.quantity,
                    security: data.security,
                    side: data.side,
                    state: data.state,
                    updated: data.updated
                }],
                addIndex: 0
            });
        }
    };

    const onGridReady = (params: GridReadyEvent) => {
        gridApiRef.current = params.api;
        params.api.sizeColumnsToFit();
    };

    return (
        <div>
            <h5>Trades</h5>
            <div className="ag-theme-alpine" style={{ width: '100%', height: 350 }}>
                <AgGridReact
                    columnDefs={columnDefs}
                    rowData={trades}
                    onGridReady={onGridReady}
                    getRowId={getRowId}
                    suppressColumnVirtualisation
                />
            </div>
        </div>
    );
}

export default TradeBlotter;
