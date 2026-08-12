package finos.traderx.tradeservice.audit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final int LOG_EXCERPT_LENGTH = 100;

    private final OrderDecisionAuditRepository repository;
    private final BestExecutionAuditProperties properties;
    private final Clock clock;
    private final AtomicReference<Instant> lastRecordedAt = new AtomicReference<>();

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
        // Configured values go through fitToColumn as a last resort only; an over-long one is
        // refused at startup, because a truncated limit id names a limit that matches nothing.
        EvaluatedLimit limit = new EvaluatedLimit(
                fitToColumn(properties.getLimit().getId(), LIMIT_FIELD_MAX_LENGTH),
                fitToColumn(properties.getLimit().getType(), LIMIT_FIELD_MAX_LENGTH),
                properties.getLimit().getValue(), properties.getLimit().getEffectiveFrom());

        OrderDecisionAudit auditRecord = new OrderDecisionAudit(
                UUID.randomUUID().toString(),
                fitToColumn(correlationId, ID_MAX_LENGTH),
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
                Instant.now(clock).truncatedTo(ChronoUnit.MILLIS),
                nextRecordedAt());

        OrderDecisionAudit saved = repository.save(auditRecord);
        log.info("Best-execution audit record written: {}", saved);
        return saved;
    }

    /**
     * The regulatory timestamp is truncated to milliseconds, so an accepted order and the
     * dispatch failure that follows it can share one. This is the ordering key: strictly
     * increasing within the process that wrote the pair, at the microsecond resolution the
     * column stores, so the sequence of records under one correlation id is unambiguous.
     */
    private Instant nextRecordedAt() {
        return lastRecordedAt.updateAndGet(previous -> {
            Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
            return previous == null || now.isAfter(previous) ? now : previous.plus(1, ChronoUnit.MICROS);
        });
    }

    /**
     * Client supplied values are recorded as far as the column allows rather than being
     * allowed to fail the insert: a refusal to store the record is the one outcome the
     * regulatory trail cannot have. Configured values pass through here too, but cannot
     * reach it overlong - {@link BestExecutionAuditProperties} refuses those at startup,
     * where the operator can still act on them.
     */
    private String fitToColumn(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        // Only an excerpt: the value can be a client header of unbounded length, and a single
        // log line long enough to be a problem in itself helps no one reconstruct anything.
        log.warn("Truncating a {} character value to {} for the audit record; it began [{}]", value.length(),
                maxLength, forLogging(value.substring(0, Math.min(value.length(), LOG_EXCERPT_LENGTH))));
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
