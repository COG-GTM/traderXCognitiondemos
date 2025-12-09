import React, { useCallback, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';

import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { SelectChangeEvent } from '@mui/material';
import { socket } from '../socket';
import { GetPositions, GetTrades } from '../hooks';
import { CreateAccount, CreateAccountUser, CreateTradeButton } from '../ActionButtons';
import { ColDef } from 'ag-grid-community';
import { PositionData, TradeData } from './types';
import { AccountsDropdown } from '../AccountsDropdown';

/** Socket.IO event name for receiving published messages */
const PUBLISH='publish';
/** Socket.IO event name for subscribing to topics */
const SUBSCRIBE='subscribe';
/** Socket.IO event name for unsubscribing from topics */
const UNSUBSCRIBE='unsubscribe';

/**
 * Datatable Component
 * 
 * A React functional component that serves as the main trading dashboard.
 * This component was migrated from a combination of Angular trade-blotter and position-blotter components.
 * 
 * @description
 * Renders the main trading interface consisting of:
 * - Account selector dropdown
 * - Action buttons (Create Trade, Create Account, Create Account User)
 * - Trade blotter (AG Grid table showing trades)
 * - Position blotter (AG Grid table showing positions)
 * 
 * The component manages real-time updates via Socket.IO subscriptions:
 * - Subscribes to trade and position updates for the selected account
 * - Automatically unsubscribes when account changes
 * - Updates grid data in real-time as new trades/positions arrive
 * 
 * @migration
 * - Angular Source: Combination of trade-blotter.component.ts and position-blotter.component.ts
 * - Angular PositionService replaced with GetPositions and GetTrades custom hooks
 * - Angular TradeFeedService replaced with direct Socket.IO integration
 * - Angular ngOnInit/ngOnDestroy lifecycle replaced with useEffect hooks
 * - Angular @Input() bindings replaced with internal state management
 * - RxJS subscriptions replaced with Socket.IO event listeners
 * - Angular AG Grid module replaced with ag-grid-react
 * 
 * @example
 * ```tsx
 * <Datatable />
 * ```
 * 
 * @returns {JSX.Element} The main trading dashboard with blotters and action buttons
 */
export const Datatable = () => {
	const [tradeRowData, setTradeRowData] = useState<TradeData[]>([]);
	const [tradeColumnDefs, setTradeColumnDefs] = useState<ColDef[]>([]);
	const [positionRowData, setPositionRowData] = useState<PositionData[]>([]);
	const [positionColumnDefs, setPositionColumnDefs] = useState<ColDef[]>([]);
	const [selectedId, setSelectedId] = useState<number>(0);
	const [currentAccount, setCurrentAccount] = useState<string>('');

	const positionData = GetPositions(selectedId);
	const tradeData = GetTrades(selectedId);

	const handleChange = useCallback((event:SelectChangeEvent<any>) => {
		socket.off(PUBLISH);
		if (selectedId !== 0){
			socket.emit(UNSUBSCRIBE,`/accounts/${selectedId}/trades`);
			socket.emit(UNSUBSCRIBE,`/accounts/${selectedId}/positions`);
		}
		setSelectedId(event.target.value);
		setCurrentAccount(event.target.value);
		socket.emit(SUBSCRIBE,`/accounts/${event.target.value}/trades`);
		socket.emit(SUBSCRIBE,`/accounts/${event.target.value}/positions`);
		socket.on(PUBLISH, (data:any) => {
			if (data.topic === `/accounts/${event.target.value}/trades`) {
				console.log("INCOMING TRADE DATA: ", data);
				setTradeRowData((current: TradeData[]) => [...current, data.payload]);
			}
			if (data.topic === `/accounts/${event.target.value}/positions`) {
				console.log("INCOMING POSITION DATA: ", data);
				setPositionRowData((current: PositionData[]) => [...current, data.payload]);
			}
		});
  }, [selectedId])

	useEffect(() => {
			const positionKeys = ['security','quantity','updated'];
			const tradeKeys = ['security','quantity','side','state','updated'];
			setPositionRowData(positionData);
			setTradeRowData(tradeData);
			setPositionColumnDefs([])
			setTradeColumnDefs([]);
			positionKeys.forEach((key:string) => setPositionColumnDefs((current: ColDef<PositionData>[]) => [...current, {field: key}]));
			tradeKeys.forEach((key:string) => setTradeColumnDefs((current: ColDef<TradeData>[]) => [...current, {field: key}]));
	}, [positionData, tradeData, selectedId, currentAccount])


return (
	<>
		<div className="accounts-dropdown">
			<AccountsDropdown currentAccount={currentAccount} handleChange={handleChange} />
		</div>
		<div className="action-buttons" style={{width: "100%", display: "flex"}}>
			<CreateTradeButton accountId={selectedId} />
			<CreateAccount />
			<CreateAccountUser accountId={selectedId} />
		</div>
		<div className="ag-theme-alpine" style={{height: "80vh", width: "50%", float: "left"}}>
				<AgGridReact
						rowData={tradeRowData}
						columnDefs={tradeColumnDefs}>
				</AgGridReact>
		</div>
		<div className="ag-theme-alpine" style={{height: "80vh", width: "50%", float: "right"}}>
			<AgGridReact
					rowData={positionRowData}
					columnDefs={positionColumnDefs}>
			</AgGridReact>
		</div>
	</>
);
}
