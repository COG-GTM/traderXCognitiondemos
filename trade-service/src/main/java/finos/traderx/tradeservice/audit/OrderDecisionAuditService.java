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
    private static final int LIMIT_FIELD_MAX_LENGTH = 40;
    private static final int PRICE_SOURCE_MAX_LENGTH = 40;
    private static final int SUBMITTED_BY_MAX_LENGTH = 50;

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
        // Configured values are fitted too: a misconfigured limit name must not be able to fail
        // the insert, because that would fail every order submission rather than one record.
        EvaluatedLimit limit = new EvaluatedLimit(
                fitToColumn(properties.getLimit().getId(), LIMIT_FIELD_MAX_LENGTH),
                fitToColumn(properties.getLimit().getType(), LIMIT_FIELD_MAX_LENGTH),
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
                fitToColumn(properties.getPricing().getSource(), PRICE_SOURCE_MAX_LENGTH),
                notional,
                decision,
                reason,
                limit,
                fitToColumn(submittedBy, SUBMITTED_BY_MAX_LENGTH),
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
        log.warn("Truncating value to {} characters for the audit record; original was [{}]", maxLength,
                forLogging(value));
        return value.substring(0, maxLength);
    }

    /**
     * Client supplied text reaches the operational log, which is itself part of how a decision
     * gets reconstructed, so line breaks are neutralised rather than allowed to forge entries.
     */
    private String forLogging(String value) {
        return value.replaceAll("[\\r\\n]", "_");
    }

    private BigDecimal notionalOf(TradeOrder order, BigDecimal price) {
        if (order.getQuantity() == null || price == null) {
            return null;
        }
        return price.multiply(BigDecimal.valueOf(order.getQuantity()));
    }
}
