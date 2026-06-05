import { ComplianceStatus, TradeOrder } from './interfaces/trade-order.interface';

/**
 * The default compliance status applied to a trade order when none is supplied,
 * matching the trade-service contract default.
 */
export const DEFAULT_COMPLIANCE_STATUS = ComplianceStatus.PENDING_REVIEW;

/**
 * Ensures a trade order carries a compliance status, defaulting to
 * PENDING_REVIEW when it is missing.
 */
export function withDefaultCompliance(
  order: Partial<TradeOrder> & Omit<TradeOrder, 'complianceStatus'>,
): TradeOrder {
  return {
    ...order,
    complianceStatus: order.complianceStatus ?? DEFAULT_COMPLIANCE_STATUS,
  };
}

/**
 * A trade is considered cleared for processing only once it has been APPROVED.
 */
export function isCompliant(order: Pick<TradeOrder, 'complianceStatus'>): boolean {
  return order.complianceStatus === ComplianceStatus.APPROVED;
}
