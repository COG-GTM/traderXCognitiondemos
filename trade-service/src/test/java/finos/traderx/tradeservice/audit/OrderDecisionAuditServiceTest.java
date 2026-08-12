package finos.traderx.tradeservice.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import finos.traderx.tradeservice.model.audit.DecisionOutcome;
import finos.traderx.tradeservice.model.audit.DecisionReason;
import finos.traderx.tradeservice.model.audit.OrderDecisionAudit;
import finos.traderx.tradeservice.repository.OrderDecisionAuditRepository;

class OrderDecisionAuditServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-12T09:15:30.123456Z");

    private OrderDecisionAuditRepository repository;
    private BestExecutionAuditProperties properties;
    private OrderDecisionAuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderDecisionAuditRepository.class);
        when(repository.save(any(OrderDecisionAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        properties = new BestExecutionAuditProperties();
        properties.getPricing().setReferencePrice(new BigDecimal("12.5000"));
        properties.getLimit().setValue(new BigDecimal("1000000.0000"));
        properties.getLimit().setEffectiveFrom(Instant.parse("2025-04-01T00:00:00Z"));
        service = new OrderDecisionAuditService(repository, properties,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsAnAcceptedDecisionWithNotionalPriceAndLimitSnapshot() {
        TradeOrder order = new TradeOrder("ORDER-1", 22214, "IBM", TradeSide.Buy, 400);

        OrderDecisionAudit record = service.recordDecision(order, "CORR-1", DecisionOutcome.ACCEPTED,
                DecisionReason.VALIDATED, "user01");

        assertNotNull(record.getId());
        assertEquals("CORR-1", record.getCorrelationId());
        assertEquals("ORDER-1", record.getOrderId());
        assertEquals(22214, record.getAccountId());
        assertEquals("IBM", record.getSecurity());
        assertEquals("Buy", record.getSide());
        assertEquals(400, record.getQuantity());
        assertEquals(new BigDecimal("12.5000"), record.getPrice());
        assertEquals(0, new BigDecimal("5000.0000").compareTo(record.getNotional()));
        assertEquals("CONFIGURED_STATIC_REFERENCE_PRICE", record.getPriceSource());
        assertEquals(DecisionOutcome.ACCEPTED, record.getDecision());
        assertEquals(DecisionReason.VALIDATED, record.getReasonCode());
        assertEquals("DEFAULT-ACCOUNT-NOTIONAL", record.getLimitId());
        assertEquals("ACCOUNT_NOTIONAL", record.getLimitType());
        assertEquals(new BigDecimal("1000000.0000"), record.getLimitValue());
        assertEquals(Instant.parse("2025-04-01T00:00:00Z"), record.getLimitEffectiveFrom());
        assertEquals("user01", record.getSubmittedBy());
        assertEquals(Instant.parse("2026-08-12T09:15:30.123Z"), record.getDecisionTimestamp());
    }

    @Test
    void recordsARejectedDecisionForAnOrderThatNeverExists() {
        TradeOrder order = new TradeOrder("ORDER-2", 99999, "NOSUCH", TradeSide.Sell, 10);

        service.recordDecision(order, "CORR-2", DecisionOutcome.REJECTED, DecisionReason.ACCOUNT_NOT_FOUND, "user03");

        ArgumentCaptor<OrderDecisionAudit> captor = ArgumentCaptor.forClass(OrderDecisionAudit.class);
        verify(repository).save(captor.capture());
        OrderDecisionAudit record = captor.getValue();
        assertEquals(DecisionOutcome.REJECTED, record.getDecision());
        assertEquals(DecisionReason.ACCOUNT_NOT_FOUND, record.getReasonCode());
        assertEquals(99999, record.getAccountId());
        assertEquals("CORR-2", record.getCorrelationId());
        assertEquals("user03", record.getSubmittedBy());
    }

    @Test
    void twoRecordsForOneOrderGetADistinctOrderingKeyEvenOnTheSameClockTick() {
        TradeOrder order = new TradeOrder("ORDER-3", 22214, "IBM", TradeSide.Buy, 10);

        OrderDecisionAudit accepted = service.recordDecision(order, "CORR-3", DecisionOutcome.ACCEPTED,
                DecisionReason.VALIDATED, "user04");
        OrderDecisionAudit dispatchFailed = service.recordDecision(order, "CORR-3", DecisionOutcome.REJECTED,
                DecisionReason.DISPATCH_FAILED, "user04");

        assertEquals(accepted.getDecisionTimestamp(), dispatchFailed.getDecisionTimestamp());
        assertTrue(dispatchFailed.getRecordedAt().isAfter(accepted.getRecordedAt()));
    }

    @Test
    void fitsOverlongValuesToTheirColumnsRatherThanFailingTheInsert() {
        TradeOrder order = new TradeOrder("O".repeat(80), 22214, "S".repeat(80), TradeSide.Buy, 1);

        OrderDecisionAudit record = service.recordDecision(order, "CORR-4", DecisionOutcome.REJECTED,
                DecisionReason.SUBMISSION_INVALID, "u".repeat(200));

        assertEquals(50, record.getOrderId().length());
        assertEquals(50, record.getSecurity().length());
        assertEquals(50, record.getSubmittedBy().length());
    }

    @Test
    void writesNothingWhenTheFeatureFlagIsOff() {
        properties.setEnabled(false);

        assertNull(service.recordDecision(new TradeOrder("ORDER-3", 22214, "IBM", TradeSide.Buy, 1), "CORR-3",
                DecisionOutcome.ACCEPTED, DecisionReason.VALIDATED, "user01"));
        verify(repository, never()).save(any());
    }

    @Test
    void auditRecordExposesNoMutatorsAndTheRepositoryExposesNoDeletes() {
        for (Method method : OrderDecisionAudit.class.getDeclaredMethods()) {
            assertTrue(!method.getName().startsWith("set"),
                    "OrderDecisionAudit must stay append-only but declares " + method.getName());
        }
        for (Method method : OrderDecisionAuditRepository.class.getMethods()) {
            assertTrue(!method.getName().toLowerCase().contains("delete")
                    && !method.getName().toLowerCase().contains("remove"),
                    "Audit repository must expose no delete path but declares " + method.getName());
        }
    }
}
