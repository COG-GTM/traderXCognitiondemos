export type TradeSide = 'Buy' | 'Sell';

export type ComplianceStatus = 'PENDING_REVIEW' | 'APPROVED' | 'FLAGGED' | 'REJECTED';

export interface TradeData {
	id?: string;
	accountId?: number;
	security: string;
	side?: TradeSide;
	state?: string;
	complianceStatus?: ComplianceStatus;
	quantity: number;
	updated?: Date;
	created?: Date;
}

export interface PositionData {
	accountId: number;
	security: string;
	quantity: number;
	updated: Date;
}