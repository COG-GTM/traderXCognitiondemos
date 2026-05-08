package finos.traderx.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeSideTest {

    @Test
    void hasBuyAndSellValues() {
        assertEquals(2, TradeSide.values().length);
        assertNotNull(TradeSide.valueOf("Buy"));
        assertNotNull(TradeSide.valueOf("Sell"));
    }
}
