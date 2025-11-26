package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TradeSideTest {

    @Test
    void buy_EnumValueExists() {
        TradeSide side = TradeSide.Buy;
        
        assertNotNull(side);
        assertEquals("Buy", side.name());
    }

    @Test
    void sell_EnumValueExists() {
        TradeSide side = TradeSide.Sell;
        
        assertNotNull(side);
        assertEquals("Sell", side.name());
    }

    @Test
    void values_ContainsBothBuyAndSell() {
        TradeSide[] values = TradeSide.values();
        
        assertEquals(2, values.length);
        assertTrue(containsValue(values, TradeSide.Buy));
        assertTrue(containsValue(values, TradeSide.Sell));
    }

    @Test
    void valueOf_Buy_ReturnsCorrectEnum() {
        TradeSide side = TradeSide.valueOf("Buy");
        
        assertEquals(TradeSide.Buy, side);
    }

    @Test
    void valueOf_Sell_ReturnsCorrectEnum() {
        TradeSide side = TradeSide.valueOf("Sell");
        
        assertEquals(TradeSide.Sell, side);
    }

    @Test
    void valueOf_InvalidValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> TradeSide.valueOf("Invalid"));
    }

    @Test
    void ordinal_Buy_IsZero() {
        assertEquals(0, TradeSide.Buy.ordinal());
    }

    @Test
    void ordinal_Sell_IsOne() {
        assertEquals(1, TradeSide.Sell.ordinal());
    }

    private boolean containsValue(TradeSide[] values, TradeSide target) {
        for (TradeSide value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
