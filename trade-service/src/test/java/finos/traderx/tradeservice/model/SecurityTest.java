package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SecurityTest {

    @Test
    void constructor_WithParameters_SetsAllFields() {
        Security security = new Security("AAPL", "Apple Inc.");

        assertEquals("AAPL", security.getTicker());
        assertEquals("Apple Inc.", security.getcompanyName());
    }

    @Test
    void defaultConstructor_CreatesEmptyObject() {
        Security security = new Security();

        assertNull(security.getTicker());
        assertNull(security.getcompanyName());
    }

    @Test
    void getTicker_ReturnsCorrectTicker() {
        Security security = new Security("MSFT", "Microsoft Corporation");

        assertEquals("MSFT", security.getTicker());
    }

    @Test
    void getcompanyName_ReturnsCorrectCompanyName() {
        Security security = new Security("GOOG", "Alphabet Inc.");

        assertEquals("Alphabet Inc.", security.getcompanyName());
    }

    @Test
    void constructor_WithDifferentSecurities_WorksCorrectly() {
        Security security1 = new Security("TSLA", "Tesla Inc.");
        Security security2 = new Security("AMZN", "Amazon.com Inc.");

        assertEquals("TSLA", security1.getTicker());
        assertEquals("Tesla Inc.", security1.getcompanyName());
        assertEquals("AMZN", security2.getTicker());
        assertEquals("Amazon.com Inc.", security2.getcompanyName());
    }

    @Test
    void constructor_WithEmptyCompanyName_WorksCorrectly() {
        Security security = new Security("TEST", "");

        assertEquals("TEST", security.getTicker());
        assertEquals("", security.getcompanyName());
    }

    @Test
    void constructor_WithNullCompanyName_WorksCorrectly() {
        Security security = new Security("TEST", null);

        assertEquals("TEST", security.getTicker());
        assertNull(security.getcompanyName());
    }

    @Test
    void constructor_WithNullTicker_WorksCorrectly() {
        Security security = new Security(null, "Test Company");

        assertNull(security.getTicker());
        assertEquals("Test Company", security.getcompanyName());
    }
}
