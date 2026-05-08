package finos.traderx.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeOrderTest {

    @Test
    void defaultConstructorCreatesEmptyOrder() {
        TradeOrder order = new TradeOrder();
        assertNull(order.getId());
        assertNull(order.getAccountId());
        assertNull(order.getSecurity());
        assertNull(order.getSide());
        assertNull(order.getQuantity());
    }

    @Test
    void parameterizedConstructorSetsAllFields() {
        TradeOrder order = new TradeOrder("order-1", 12345, "AAPL", TradeSide.Buy, 100);

        assertEquals("order-1", order.getId());
        assertEquals(12345, order.getAccountId());
        assertEquals("AAPL", order.getSecurity());
        assertEquals(TradeSide.Buy, order.getSide());
        assertEquals(100, order.getQuantity());
    }

    @Test
    void sellSideIsPreserved() {
        TradeOrder order = new TradeOrder("order-2", 99, "MSFT", TradeSide.Sell, 50);
        assertEquals(TradeSide.Sell, order.getSide());
    }
}
