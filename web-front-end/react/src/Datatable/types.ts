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