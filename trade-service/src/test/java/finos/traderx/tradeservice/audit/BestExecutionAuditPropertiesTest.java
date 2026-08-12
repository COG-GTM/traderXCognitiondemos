package finos.traderx.tradeservice.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * A number too large for its audit column would fail every insert, and the audit write is
 * deliberately fail-closed, so it must be refused at startup rather than at the first order.
 */
class BestExecutionAuditPropertiesTest {

    @Test
    void acceptsTheConfiguredDevDefaults() {
        assertDoesNotThrow(new BestExecutionAuditProperties()::validate);
    }

    @Test
    void normalisesTheConfiguredPriceToTheScaleItIsStoredAt() {
        BestExecutionAuditProperties properties = new BestExecutionAuditProperties();
        properties.getPricing().setReferencePrice(new BigDecimal("100.00005"));

        properties.validate();

        assertEquals(new BigDecimal("100.0001"), properties.getPricing().getReferencePrice());
    }

    @Test
    void refusesALimitTooLargeForTheAuditColumn() {
        BestExecutionAuditProperties properties = new BestExecutionAuditProperties();
        properties.getLimit().setValue(new BigDecimal("1".repeat(20)));

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void refusesAPriceThatFitsItsOwnColumnButOverflowsNotionalAtMaxQuantity() {
        BestExecutionAuditProperties properties = new BestExecutionAuditProperties();
        properties.getPricing().setReferencePrice(new BigDecimal("1".repeat(15)));

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void refusesAReferencePriceTooLargeForTheAuditColumn() {
        BestExecutionAuditProperties properties = new BestExecutionAuditProperties();
        properties.getPricing().setReferencePrice(new BigDecimal("1".repeat(16)));

        assertThrows(IllegalStateException.class, properties::validate);
    }
}
