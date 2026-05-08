package finos.traderx.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeStateTest {

    @Test
    void hasFourStates() {
        assertEquals(4, TradeState.values().length);
        assertNotNull(TradeState.valueOf("New"));
        assertNotNull(TradeState.valueOf("Processing"));
        assertNotNull(TradeState.valueOf("Settled"));
        assertNotNull(TradeState.valueOf("Cancelled"));
    }
}
