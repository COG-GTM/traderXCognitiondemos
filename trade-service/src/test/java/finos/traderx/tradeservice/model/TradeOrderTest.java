package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TradeOrderTest {

    @Test
    void constructor_WithAllParameters_SetsAllFields() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);

        assertEquals("trade-123", tradeOrder.getId());
        assertEquals(100001, tradeOrder.getAccountId());
        assertEquals("AAPL", tradeOrder.getSecurity());
        assertEquals(TradeSide.Buy, tradeOrder.getSide());
        assertEquals(100, tradeOrder.getQuantity());
    }

    @Test
    void defaultConstructor_CreatesEmptyObject() {
        TradeOrder tradeOrder = new TradeOrder();

        assertNull(tradeOrder.getId());
        assertNull(tradeOrder.getAccountId());
        assertNull(tradeOrder.getSecurity());
        assertNull(tradeOrder.getSide());
        assertNull(tradeOrder.getQuantity());
        assertNull(tradeOrder.getState());
    }

    @Test
    void getId_ReturnsCorrectId() {
        TradeOrder tradeOrder = new TradeOrder("unique-id-456", 100001, "MSFT", TradeSide.Sell, 50);

        assertEquals("unique-id-456", tradeOrder.getId());
    }

    @Test
    void getAccountId_ReturnsCorrectAccountId() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 200002, "GOOG", TradeSide.Buy, 25);

        assertEquals(200002, tradeOrder.getAccountId());
    }

    @Test
    void getSecurity_ReturnsCorrectSecurity() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "TSLA", TradeSide.Buy, 10);

        assertEquals("TSLA", tradeOrder.getSecurity());
    }

    @Test
    void getSide_ReturnsBuy() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);

        assertEquals(TradeSide.Buy, tradeOrder.getSide());
    }

    @Test
    void getSide_ReturnsSell() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Sell, 100);

        assertEquals(TradeSide.Sell, tradeOrder.getSide());
    }

    @Test
    void getQuantity_ReturnsCorrectQuantity() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 500);

        assertEquals(500, tradeOrder.getQuantity());
    }

    @Test
    void getState_ReturnsNullForNewOrder() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);

        assertNull(tradeOrder.getState());
    }
}
