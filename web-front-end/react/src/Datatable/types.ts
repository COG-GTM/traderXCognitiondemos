export type TradeSide = 'Buy' | 'Sell';

export interface TradeData {
	id?: string;
	accountId?: number;
	security: string;
	side?: TradeSide;
	state?: string;
	quantity: number;
	updated?: Date;
	created?: Date;
}

/** Legacy v1 row shape from GET /positions/{accountId}. currency/marketValue were added for the report. */
export interface PositionData {
	accountId: number;
	security: string;
	quantity: number;
	updated: Date;
	currency?: string;
	marketValue?: number | null;
}

/** v2 market value: decimal amount as a STRING at the currency's minor-unit scale. Never a float. */
export interface PositionMarketValue {
	amount: string;
	currency: string;
}

/** v2 position item from GET /v2/positions. `marketValue` is null when unknown; `currency` is always present. */
export interface Position {
	accountId: number;
	security: string;
	quantity: number;
	asOf: string; // ISO-8601 UTC instant, e.g. 2026-09-22T16:00:00Z
	currency: string;
	marketValue: PositionMarketValue | null;
}

/** One v2 page: `nextCursor` is an opaque string, or null on the final page. */
export interface PositionPage {
	items: Position[];
	nextCursor: string | null;
}
