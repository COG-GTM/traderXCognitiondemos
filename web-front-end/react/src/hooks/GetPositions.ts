import { SetStateAction, useEffect, useState } from "react";
import { PositionData } from "../Datatable/types";
import { Environment } from '../env';

/**
 * GetPositions Custom Hook
 * 
 * A React custom hook that fetches and returns positions for a specific account.
 * This hook was migrated from the Angular PositionService.getPositions() method.
 * 
 * @description
 * Fetches position data from the position service API whenever the accountId changes.
 * Returns an array of PositionData objects representing the account's current positions.
 * The fetch is re-triggered whenever the accountId dependency changes.
 * 
 * @migration
 * - Angular Source: PositionService.getPositions() in service/position.service.ts
 * - Angular @Injectable service replaced with custom hook pattern
 * - RxJS Observable with catchError replaced with async/await and try/catch
 * - Angular HttpClient replaced with fetch API
 * - Angular dependency injection replaced with direct hook usage
 * - Observable subscription replaced with useEffect dependency array
 * 
 * @param {number} accountId - The ID of the account to fetch positions for
 * 
 * @example
 * ```tsx
 * const positions = GetPositions(12345);
 * // positions is PositionData[]
 * ```
 * 
 * @returns {PositionData[]} Array of position objects for the specified account
 */
export const GetPositions = (accountId:number) => {
	const [positionsData, setPositionsData] = useState<PositionData[]>([]);
	type data = () => Promise<unknown>;
	useEffect(() => {
		let json:SetStateAction<PositionData[]>;
		const fetchData: data = async () => {
			try {
				const response = await fetch(`${Environment.position_service_url}/positions/${accountId}`);
				if (response.ok) {
					json = await response.json();
					setPositionsData(json);
				}
			} catch (error) {
				return error;
			}
		};
		fetchData()
	}, [accountId]);
	return positionsData;
}
