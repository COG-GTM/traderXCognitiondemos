import { useEffect, useState } from "react";
import { Position, PositionPage } from "../Datatable/types";
import { Environment } from '../env';

export const V2_POSITIONS_MAX_LIMIT = 100;

const buildPageUrl = (accountId: number, cursor: string | null, limit?: number): string => {
	const params = new URLSearchParams({ accountId: String(accountId) });
	if (cursor !== null) {
		params.set('cursor', cursor);
	}
	if (limit !== undefined) {
		params.set('limit', String(limit));
	}
	return `${Environment.position_service_url}/v2/positions?${params.toString()}`;
};

/**
 * Walks every page of GET /v2/positions for one account and returns the concatenated items.
 * The cursor is opaque and is followed verbatim; the walk stops only when `nextCursor` is null.
 * `limit` is optional (service default when omitted); the result is the same for any limit in 1..100.
 */
export const fetchAllPositions = async (accountId: number, limit?: number): Promise<Position[]> => {
	if (limit !== undefined && (!Number.isInteger(limit) || limit < 1 || limit > V2_POSITIONS_MAX_LIMIT)) {
		throw new Error(`limit must be an integer between 1 and ${V2_POSITIONS_MAX_LIMIT}`);
	}
	const items: Position[] = [];
	let cursor: string | null = null;
	do {
		const response = await fetch(buildPageUrl(accountId, cursor, limit));
		if (!response.ok) {
			throw new Error(`Positions v2 request failed: ${response.status}`);
		}
		const page: PositionPage = await response.json();
		items.push(...page.items);
		cursor = page.nextCursor;
	} while (cursor !== null);
	return items;
};

export const GetPositions = (accountId: number, limit?: number) => {
	const [positionsData, setPositionsData] = useState<Position[]>([]);
	useEffect(() => {
		if (!accountId) {
			setPositionsData([]);
			return;
		}
		let cancelled = false;
		fetchAllPositions(accountId, limit)
			.then((positions) => {
				if (!cancelled) {
					setPositionsData(positions);
				}
			})
			.catch(() => {
				if (!cancelled) {
					setPositionsData([]);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [accountId, limit]);
	return positionsData;
}
