package finos.traderx.tradeservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import finos.traderx.tradeservice.model.audit.DecisionOutcome;
import finos.traderx.tradeservice.model.audit.DecisionReason;
import finos.traderx.tradeservice.model.audit.EvaluatedLimit;
import finos.traderx.tradeservice.model.audit.OrderDecisionAudit;

@DataJpaTest
class OrderDecisionAuditRepositoryTest {

    @Autowired
    private OrderDecisionAuditRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private OrderDecisionAudit rejectedRecord(String id, String correlationId) {
        return new OrderDecisionAudit(id, correlationId, "ORDER-" + id, 22214, "IBM", "Buy", 100,
                new BigDecimal("10.0000"), "CONFIGURED_STATIC_REFERENCE_PRICE", new BigDecimal("1000.0000"),
                DecisionOutcome.REJECTED, DecisionReason.SECURITY_NOT_FOUND,
                new EvaluatedLimit("DEFAULT-ACCOUNT-NOTIONAL", "ACCOUNT_NOTIONAL", new BigDecimal("5000000.0000"),
                        Instant.parse("2024-01-01T00:00:00Z")),
                "user01", Instant.parse("2026-08-12T09:15:30.123Z"));
    }

    @Test
    void persistsAndRetrievesADecisionByCorrelationId() {
        repository.save(rejectedRecord("A1", "CORR-A"));
        entityManager.flush();
        entityManager.clear();

        List<OrderDecisionAudit> found = repository.findByCorrelationId("CORR-A");

        assertEquals(1, found.size());
        assertEquals(DecisionOutcome.REJECTED, found.get(0).getDecision());
        assertEquals(Instant.parse("2026-08-12T09:15:30.123Z"), found.get(0).getDecisionTimestamp());
    }

    @Test
    void aPersistedDecisionCannotBeUpdated() throws Exception {
        OrderDecisionAudit saved = repository.save(rejectedRecord("A2", "CORR-B"));
        entityManager.flush();

        Field decision = OrderDecisionAudit.class.getDeclaredField("decision");
        decision.setAccessible(true);
        decision.set(saved, DecisionOutcome.ACCEPTED);
        entityManager.flush();
        entityManager.clear();

        OrderDecisionAudit reloaded = repository.findById("A2").orElseThrow();
        assertEquals(DecisionOutcome.REJECTED, reloaded.getDecision());
        assertTrue(reloaded.isNew());
    }
}
