import {
  DEFAULT_COMPLIANCE_STATUS,
  isCompliant,
  withDefaultCompliance,
} from './compliance.util';
import { ComplianceStatus } from './interfaces/trade-order.interface';

describe('compliance util', () => {
  const baseOrder = {
    id: 'TRADE-1',
    accountId: 22214,
    security: 'AAPL',
    side: 'Buy' as const,
    quantity: 100,
  };

  it('defaults to PENDING_REVIEW', () => {
    expect(DEFAULT_COMPLIANCE_STATUS).toBe(ComplianceStatus.PENDING_REVIEW);
    expect(withDefaultCompliance(baseOrder).complianceStatus).toBe(
      ComplianceStatus.PENDING_REVIEW,
    );
  });

  it('preserves an explicit compliance status', () => {
    const order = withDefaultCompliance({
      ...baseOrder,
      complianceStatus: ComplianceStatus.APPROVED,
    });
    expect(order.complianceStatus).toBe(ComplianceStatus.APPROVED);
  });

  it('only treats APPROVED orders as compliant', () => {
    expect(isCompliant({ complianceStatus: ComplianceStatus.APPROVED })).toBe(true);
    expect(isCompliant({ complianceStatus: ComplianceStatus.PENDING_REVIEW })).toBe(
      false,
    );
    expect(isCompliant({ complianceStatus: ComplianceStatus.FLAGGED })).toBe(false);
    expect(isCompliant({ complianceStatus: ComplianceStatus.REJECTED })).toBe(false);
  });
});
