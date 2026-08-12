package finos.traderx.tradeservice.audit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.audit.DecisionOutcome;
import finos.traderx.tradeservice.model.audit.DecisionReason;
import finos.traderx.tradeservice.model.audit.EvaluatedLimit;
import finos.traderx.tradeservice.model.audit.OrderDecisionAudit;
import finos.traderx.tradeservice.repository.OrderDecisionAuditRepository;

/**
 * Writes the append-only best-execution record for every order submission.
 *
 * The write is synchronous with the decision and commits in its own transaction, so the
 * record survives a later failure in the same request (a publish error, for instance).
 */
@Service
public class OrderDecisionAuditService {

    private static final Logger log = LoggerFactory.getLogger(OrderDecisionAuditService.class);

    private static final int ID_MAX_LENGTH = 50;
    private static final int SECURITY_MAX_LENGTH = 50;

    private final OrderDecisionAuditRepository repository;
    private final BestExecutionAuditProperties properties;
    private final Clock clock;

    public OrderDecisionAuditService(OrderDecisionAuditRepository repository,
            BestExecutionAuditProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDecisionAudit recordDecision(TradeOrder order, String correlationId, DecisionOutcome decision,
            DecisionReason reason, String submittedBy) {
        if (!properties.isEnabled()) {
            log.debug("Best-execution audit disabled; no record written for correlation id {}", correlationId);
            return null;
        }

        BigDecimal price = properties.getPricing().getReferencePrice();
        BigDecimal notional = notionalOf(order, price);
        EvaluatedLimit limit = new EvaluatedLimit(properties.getLimit().getId(), properties.getLimit().getType(),
                properties.getLimit().getValue(), properties.getLimit().getEffectiveFrom());

        OrderDecisionAudit auditRecord = new OrderDecisionAudit(
                UUID.randomUUID().toString(),
                correlationId,
                fitToColumn(order.getId(), ID_MAX_LENGTH),
                order.getAccountId(),
                fitToColumn(order.getSecurity(), SECURITY_MAX_LENGTH),
                order.getSide() == null ? null : order.getSide().toString(),
                order.getQuantity(),
                price,
                properties.getPricing().getSource(),
                notional,
                decision,
                reason,
                limit,
                submittedBy,
                Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));

        OrderDecisionAudit saved = repository.save(auditRecord);
        log.info("Best-execution audit record written: {}", saved);
        return saved;
    }

    /**
     * Client supplied values are recorded as far as the column allows rather than being
     * allowed to fail the insert: a refusal to store the record is the one outcome the
     * regulatory trail cannot have.
     */
    private String fitToColumn(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        log.warn("Truncating value of length {} to {} characters for the audit record", value.length(), maxLength);
        return value.substring(0, maxLength);
    }

    private BigDecimal notionalOf(TradeOrder order, BigDecimal price) {
        if (order.getQuantity() == null || price == null) {
            return null;
        }
        return price.multiply(BigDecimal.valueOf(order.getQuantity()));
    }
}
