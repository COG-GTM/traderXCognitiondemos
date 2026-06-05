export enum ComplianceStatus {
  PENDING_REVIEW = 'PENDING_REVIEW',
  APPROVED = 'APPROVED',
  FLAGGED = 'FLAGGED',
  REJECTED = 'REJECTED',
}

export type TradeSide = 'Buy' | 'Sell';

/**
 * Shared TradeOrder data contract. `complianceStatus` is a required field that
 * mirrors the trade-service OpenAPI spec.
 */
export interface TradeOrder {
  id: string;
  accountId: number;
  security: string;
  side: TradeSide;
  quantity: number;
  state?: string;
  complianceStatus: ComplianceStatus;
}
