package finos.traderx.tradeservice.regulatory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class TradeReportingEnricherTest {

    private TradeReportingEnricher enricher;
    private RegulatoryValidator validator;

    @BeforeEach
    void setUp() {
        this.enricher = new TradeReportingEnricher();
        ReflectionTestUtils.setField(this.enricher, "leiRegistry", new LeiRegistry());
        ReflectionTestUtils.setField(this.enricher, "utiGenerator", new UtiGenerator());
        this.validator = new RegulatoryValidator();
    }

    private TradeOrder order(Integer accountId) {
        TradeOrder order = new TradeOrder();
        order.setAccountId(accountId);
        order.setSecurity("IBM");
        order.setSide(TradeSide.Buy);
        order.setQuantity(100);
        return order;
    }

    @Test
    void stampsUtiLeiAndRegimeOnAnUnenrichedOrder() {
        TradeOrder enriched = this.enricher.enrich(order(22214));

        assertEquals("549300TRADERX0ACC128", enriched.getReportingCounterpartyLei());
        assertEquals(RegulatoryRuleSet.EMIR_REFIT, enriched.getReportingRegime());
        assertEquals(UniqueTransactionIdentifiers.LENGTH, enriched.getUti().length());
        assertEquals(enriched.getReportingCounterpartyLei(),
                UniqueTransactionIdentifiers.prefixOf(enriched.getUti()));
    }

    @Test
    void enrichedOrderPassesTheRegulatoryRuleSet() {
        ValidationResult result = this.validator.validate(this.enricher.enrich(order(62654)));

        assertTrue(result.isValid(), "Enriched order should be reportable but got " + result.getRejectionCodes());
    }

    @Test
    void fallsBackToTheDefaultLeiForAnUnknownAccount() {
        TradeOrder enriched = this.enricher.enrich(order(99999));

        assertEquals(LeiRegistry.DEFAULT_LEI, enriched.getReportingCounterpartyLei());
        assertTrue(this.validator.validate(enriched).isValid());
    }

    @Test
    void preservesAnExternallySuppliedUtiAndLei() {
        TradeOrder order = order(22214);
        order.setReportingCounterpartyLei("7LTWFZYICNSX8D621K86");
        order.setUti("7LTWFZYICNSX8D621K86EXTERNALUTI0000000000000000ABCDE");

        TradeOrder enriched = this.enricher.enrich(order);

        assertEquals("7LTWFZYICNSX8D621K86", enriched.getReportingCounterpartyLei());
        assertEquals("7LTWFZYICNSX8D621K86EXTERNALUTI0000000000000000ABCDE", enriched.getUti());
        assertTrue(this.validator.validate(enriched).isValid());
    }

    @Test
    void generatesADistinctUtiPerTrade() {
        TradeOrder first = this.enricher.enrich(order(22214));
        TradeOrder second = this.enricher.enrich(order(22214));

        assertNotEquals(first.getUti(), second.getUti());
    }
}
