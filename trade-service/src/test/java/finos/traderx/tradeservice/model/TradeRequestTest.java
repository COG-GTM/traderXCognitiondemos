package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TradeRequestTest {

    @Test
    void setAndGetAccountId_WorksCorrectly() {
        TradeRequest request = new TradeRequest();
        request.setAccountId(100001);

        assertEquals(100001, request.getAccountId());
    }

    @Test
    void setAndGetSecurity_WorksCorrectly() {
        TradeRequest request = new TradeRequest();
        request.setSecurity("AAPL");

        assertEquals("AAPL", request.getSecurity());
    }

    @Test
    void setAndGetSide_Buy_WorksCorrectly() {
        TradeRequest request = new TradeRequest();
        request.setSide(TradeSide.Buy);

        assertEquals(TradeSide.Buy, request.getSide());
    }

    @Test
    void setAndGetSide_Sell_WorksCorrectly() {
        TradeRequest request = new TradeRequest();
        request.setSide(TradeSide.Sell);

        assertEquals(TradeSide.Sell, request.getSide());
    }

    @Test
    void setAndGetQuantity_WorksCorrectly() {
        TradeRequest request = new TradeRequest();
        request.setQuantity(500);

        assertEquals(500, request.getQuantity());
    }

    @Test
    void allFieldsCanBeSetAndRetrieved() {
        TradeRequest request = new TradeRequest();
        request.setAccountId(200002);
        request.setSecurity("MSFT");
        request.setSide(TradeSide.Sell);
        request.setQuantity(250);

        assertEquals(200002, request.getAccountId());
        assertEquals("MSFT", request.getSecurity());
        assertEquals(TradeSide.Sell, request.getSide());
        assertEquals(250, request.getQuantity());
    }

    @Test
    void defaultValues_AreNullOrZero() {
        TradeRequest request = new TradeRequest();

        assertEquals(0, request.getAccountId());
        assertNull(request.getSecurity());
        assertNull(request.getSide());
        assertNull(request.getQuantity());
    }
}
