package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TradeOrderTest {

    @Test
    void defaultComplianceStatusIsPendingReview() {
        TradeOrder order = new TradeOrder();
        assertEquals(ComplianceStatus.PENDING_REVIEW, order.getComplianceStatus());
    }

    @Test
    void legacyConstructorDefaultsComplianceStatus() {
        TradeOrder order = new TradeOrder("TRADE-1", 22214, "AAPL", TradeSide.Buy, 100);
        assertEquals(ComplianceStatus.PENDING_REVIEW, order.getComplianceStatus());
    }

    @Test
    void complianceStatusConstructorAndSetter() {
        TradeOrder order = new TradeOrder("TRADE-2", 22214, "AAPL", TradeSide.Sell, 50, ComplianceStatus.APPROVED);
        assertEquals(ComplianceStatus.APPROVED, order.getComplianceStatus());

        order.setComplianceStatus(ComplianceStatus.FLAGGED);
        assertEquals(ComplianceStatus.FLAGGED, order.getComplianceStatus());
    }
}
