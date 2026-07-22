import React, { useCallback, useEffect, useRef } from 'react';
import { AgGridReact } from 'ag-grid-react';
import {
	ColDef,
	GridApi,
	GridReadyEvent,
	GetRowIdParams,
} from 'ag-grid-community';

import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

import { Account, Position } from './types';
import { subscribe } from './tradeFeed';

const positionServiceUrl = `http://${window.location.hostname}:18090`;

const columnDefs: ColDef<Position>[] = [
	{ field: 'security', headerName: 'SECURITY' },
	{ field: 'quantity', headerName: 'QUANTITY', enableCellChangeFlash: true },
];

interface PositionBlotterProps {
	account?: Account;
	/** Convenience alternative to passing a full `account`. */
	accountId?: number;
}

/**
 * Live positions grid for a selected account. Hydrates from the position
 * service, then applies live updates from the trade feed. Faithfully ports
 * position-blotter.component.ts, including the pending-buffer logic for updates
 * that arrive before hydration completes.
 */
export const PositionBlotter: React.FC<PositionBlotterProps> = ({
	account,
	accountId,
}) => {
	const resolvedAccountId = account?.id ?? accountId;

	const gridApiRef = useRef<GridApi<Position> | null>(null);
	const isPendingRef = useRef<boolean>(true);
	const pendingRef = useRef<Position[]>([]);

	const applyUpdate = useCallback((data: Position) => {
		const api = gridApiRef.current;
		if (!api) {
			return;
		}
		const row = api.getRowNode(data.security);
		if (row && row.data) {
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

	const handleFeed = useCallback(
		(data: Position) => {
			if (isPendingRef.current) {
				pendingRef.current.push(data);
			} else {
				applyUpdate(data);
			}
		},
		[applyUpdate],
	);

	useEffect(() => {
		if (resolvedAccountId === undefined || resolvedAccountId === null) {
			return;
		}

		let cancelled = false;
		isPendingRef.current = true;
		pendingRef.current = [];

		const hydrate = async () => {
			try {
				const response = await fetch(
					`${positionServiceUrl}/positions/${resolvedAccountId}`,
				);
				if (cancelled) {
					return;
				}
				const positions: Position[] = response.ok
					? await response.json()
					: [];
				gridApiRef.current?.setRowData(positions);
				pendingRef.current.forEach((p) => applyUpdate(p));
				pendingRef.current = [];
				isPendingRef.current = false;
			} catch {
				isPendingRef.current = false;
			}
		};

		hydrate();

		const unsubscribe = subscribe(
			`/accounts/${resolvedAccountId}/positions`,
			handleFeed,
		);

		return () => {
			cancelled = true;
			unsubscribe();
		};
	}, [resolvedAccountId, applyUpdate, handleFeed]);

	const onGridReady = useCallback(
		(params: GridReadyEvent<Position>) => {
			gridApiRef.current = params.api;
		},
		[],
	);

	const getRowId = useCallback(
		(params: GetRowIdParams<Position>) => params.data.security,
		[],
	);

	return (
		<div
			className="ag-theme-alpine"
			style={{ height: '100%', width: '100%' }}
		>
			<AgGridReact<Position>
				columnDefs={columnDefs}
				onGridReady={onGridReady}
				getRowId={getRowId}
			/>
		</div>
	);
};

export default PositionBlotter;
