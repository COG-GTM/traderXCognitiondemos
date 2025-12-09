import { SetStateAction, useEffect, useState } from "react";
import { TradeData } from "../Datatable/types";
import { Environment } from '../env';

/**
 * GetTrades Custom Hook
 * 
 * A React custom hook that fetches and returns trades for a specific account.
 * This hook was migrated from the Angular PositionService.getTrades() method.
 * 
 * @description
 * Fetches trade data from the position service API whenever the accountId changes.
 * Returns an array of TradeData objects representing the account's trade history.
 * The fetch is re-triggered whenever the accountId dependency changes.
 * 
 * @migration
 * - Angular Source: PositionService.getTrades() in service/position.service.ts
 * - Angular @Injectable service replaced with custom hook pattern
 * - RxJS Observable with catchError replaced with async/await and try/catch
 * - Angular HttpClient replaced with fetch API
 * - Angular dependency injection replaced with direct hook usage
 * - Observable subscription replaced with useEffect dependency array
 * 
 * @param {number} accountId - The ID of the account to fetch trades for
 * 
 * @example
 * ```tsx
 * const trades = GetTrades(12345);
 * // trades is TradeData[]
 * ```
 * 
 * @returns {TradeData[]} Array of trade objects for the specified account
 */
export const GetTrades = (accountId:number) => {
	const [tradesData, setTradesData] = useState<TradeData[]>([]);
	type data = () => Promise<unknown>;

	useEffect(() => {
		let json:SetStateAction<TradeData[]>;
		const fetchData: data = async () => {
			try {
				const response = await fetch(`${Environment.position_service_url}/trades/${accountId}`);
				if (response.ok) {
					json = await response.json();
					setTradesData(json);
				}
			} catch (error) {
				return error;
			}
		};
		fetchData();
	}, [accountId]);
	return tradesData;
}
