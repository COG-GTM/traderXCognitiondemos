import React, { useCallback, useEffect, useRef } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { io, Socket } from 'socket.io-client';
import type {
	ColDef,
	GetRowIdParams,
	GridApi,
	GridReadyEvent,
} from 'ag-grid-community';

import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

import { FeedMessage, Trade } from './types';

// Backend service base URLs, computed from window.location like react/src/env.ts.
// Duplicated locally so this piece stays self-contained (see README INTEGRATION NOTE).
const POSITION_SERVICE_URL = `http://${window.location.hostname}:18090`;
const TRADE_FEED_URL = `http://${window.location.hostname}:18086`;

const PUBLISH = 'publish';
const SUBSCRIBE = 'subscribe';
const UNSUBSCRIBE = 'unsubscribe';

const columnDefs: ColDef<Trade>[] = [
	{ headerName: 'SECURITY', field: 'security' },
	{ headerName: 'QUANTITY', field: 'quantity' },
	{ headerName: 'SIDE', field: 'side' },
	{ headerName: 'STATE', field: 'state', enableCellChangeFlash: true },
	{ headerName: 'UPDATED', field: 'updated' },
];

// Match the Angular blotter's row id scheme: `Trade-${id}`.
const rowIdFor = (id: string): string => `Trade-${id}`;
const getRowId = (params: GetRowIdParams<Trade>): string => rowIdFor(params.data.id);

export interface TradeBlotterProps {
	/** The selected account id whose trades this blotter displays. */
	accountId?: number;
}

/**
 * Live trades grid for a single account. Hydrates from the position service
 * (`GET /trades/{accountId}` on :18090), then subscribes to the trade feed
 * topic `/accounts/{id}/trades` (:18086) and applies add/update grid
 * transactions keyed by trade id. Updates that arrive before hydration
 * completes are buffered and replayed once the initial rows are loaded.
 *
 * Ported from
 * web-front-end/angular/main/app/trade/trade-blotter/trade-blotter.component.ts
 */
export const TradeBlotter: React.FC<TradeBlotterProps> = ({ accountId }) => {
	const gridApiRef = useRef<GridApi<Trade> | null>(null);
	const pendingTradesRef = useRef<Trade[]>([]);
	const isPendingRef = useRef<boolean>(true);
	const socketRef = useRef<Socket | null>(null);

	// Apply a single trade update to the grid: update the existing row's state
	// if present, otherwise insert the new trade at the top.
	const update = useCallback((data: Trade) => {
		const api = gridApiRef.current;
		if (!api) {
			return;
		}
		const row = api.getRowNode(rowIdFor(data.id));
		if (row && row.data) {
			api.applyTransaction({
				update: [Object.assign(row.data, { state: data.state })],
			});
		} else {
			api.applyTransaction({
				add: [
					{
						accountid: data.accountid,
						created: data.created,
						id: data.id,
						quantity: data.quantity,
						security: data.security,
						side: data.side,
						state: data.state,
						updated: data.updated,
					},
				],
				addIndex: 0,
			});
		}
	}, []);

	// Replay any updates that arrived before hydration finished.
	const processPendingTrades = useCallback(() => {
		pendingTradesRef.current.forEach((trade) => update(trade));
		pendingTradesRef.current = [];
		isPendingRef.current = false;
	}, [update]);

	const onGridReady = useCallback((params: GridReadyEvent<Trade>) => {
		gridApiRef.current = params.api;
		params.api.sizeColumnsToFit();
	}, []);

	useEffect(() => {
		if (accountId === undefined || accountId === null) {
			return;
		}

		isPendingRef.current = true;
		pendingTradesRef.current = [];

		let cancelled = false;

		// Hydrate current trades from the position service.
		const hydrate = async () => {
			try {
				const response = await fetch(
					`${POSITION_SERVICE_URL}/trades/${accountId}`,
				);
				if (!response.ok || cancelled) {
					return;
				}
				const trades: Trade[] = await response.json();
				if (cancelled) {
					return;
				}
				gridApiRef.current?.setRowData(trades);
				processPendingTrades();
			} catch (error) {
				console.error('Trade blotter hydration failed', error);
			}
		};
		hydrate();

		// Subscribe to the live trade feed for this account.
		const topic = `/accounts/${accountId}/trades`;
		const socket = io(TRADE_FEED_URL);
		socketRef.current = socket;

		const onPublish = (message: FeedMessage<Trade>) => {
			if (message.from === 'System' || message.topic !== topic) {
				return;
			}
			const data = message.payload;
			if (isPendingRef.current) {
				pendingTradesRef.current.push(data);
			} else {
				update(data);
			}
		};

		socket.on(PUBLISH, onPublish);
		socket.emit(SUBSCRIBE, topic);

		return () => {
			cancelled = true;
			socket.emit(UNSUBSCRIBE, topic);
			socket.off(PUBLISH, onPublish);
			socket.disconnect();
			socketRef.current = null;
		};
	}, [accountId, processPendingTrades, update]);

	return (
		<>
			<h5>Trades</h5>
			<div className="ag-theme-alpine" style={{ width: '100%', height: 350 }}>
				<AgGridReact<Trade>
					columnDefs={columnDefs}
					getRowId={getRowId}
					onGridReady={onGridReady}
				/>
			</div>
		</>
	);
};

export default TradeBlotter;
