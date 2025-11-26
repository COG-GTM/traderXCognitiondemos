package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TradeStateTest {

    @Test
    void new_EnumValueExists() {
        TradeState state = TradeState.New;
        
        assertNotNull(state);
        assertEquals("New", state.name());
    }

    @Test
    void processing_EnumValueExists() {
        TradeState state = TradeState.Processing;
        
        assertNotNull(state);
        assertEquals("Processing", state.name());
    }

    @Test
    void settled_EnumValueExists() {
        TradeState state = TradeState.Settled;
        
        assertNotNull(state);
        assertEquals("Settled", state.name());
    }

    @Test
    void cancelled_EnumValueExists() {
        TradeState state = TradeState.Cancelled;
        
        assertNotNull(state);
        assertEquals("Cancelled", state.name());
    }

    @Test
    void values_ContainsAllFourStates() {
        TradeState[] values = TradeState.values();
        
        assertEquals(4, values.length);
        assertTrue(containsValue(values, TradeState.New));
        assertTrue(containsValue(values, TradeState.Processing));
        assertTrue(containsValue(values, TradeState.Settled));
        assertTrue(containsValue(values, TradeState.Cancelled));
    }

    @Test
    void valueOf_New_ReturnsCorrectEnum() {
        TradeState state = TradeState.valueOf("New");
        
        assertEquals(TradeState.New, state);
    }

    @Test
    void valueOf_Processing_ReturnsCorrectEnum() {
        TradeState state = TradeState.valueOf("Processing");
        
        assertEquals(TradeState.Processing, state);
    }

    @Test
    void valueOf_Settled_ReturnsCorrectEnum() {
        TradeState state = TradeState.valueOf("Settled");
        
        assertEquals(TradeState.Settled, state);
    }

    @Test
    void valueOf_Cancelled_ReturnsCorrectEnum() {
        TradeState state = TradeState.valueOf("Cancelled");
        
        assertEquals(TradeState.Cancelled, state);
    }

    @Test
    void valueOf_InvalidValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> TradeState.valueOf("Invalid"));
    }

    @Test
    void ordinal_ValuesAreCorrect() {
        assertEquals(0, TradeState.New.ordinal());
        assertEquals(1, TradeState.Processing.ordinal());
        assertEquals(2, TradeState.Settled.ordinal());
        assertEquals(3, TradeState.Cancelled.ordinal());
    }

    private boolean containsValue(TradeState[] values, TradeState target) {
        for (TradeState value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
