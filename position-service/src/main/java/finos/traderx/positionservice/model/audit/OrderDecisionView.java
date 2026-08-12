package finos.traderx.positionservice.model.audit;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire representation of a single retained decision.
 *
 * The entity is not serialised directly: the JSON shape is what compliance and any downstream
 * export read, and it should not change silently because a column was renamed. The limit that
 * was in force is carried by value, exactly as it was captured at decision time.
 */
public record OrderDecisionView(
        String id,
        String correlationId,
        String orderId,
        Integer accountId,
        String security,
        String side,
        Integer quantity,
        BigDecimal price,
        String priceSource,
        BigDecimal notional,
        DecisionOutcome decision,
        String reasonCode,
        String limitId,
        String limitType,
        BigDecimal limitValue,
        Instant limitEffectiveFrom,
        String submittedBy,
        Instant decisionTimestamp) {

    public static OrderDecisionView of(OrderDecisionAudit record) {
        return new OrderDecisionView(
                record.getId(),
                record.getCorrelationId(),
                record.getOrderId(),
                record.getAccountId(),
                record.getSecurity(),
                record.getSide(),
                record.getQuantity(),
                record.getPrice(),
                record.getPriceSource(),
                record.getNotional(),
                record.getDecision(),
                record.getReasonCode(),
                record.getLimitId(),
                record.getLimitType(),
                record.getLimitValue(),
                record.getLimitEffectiveFrom(),
                record.getSubmittedBy(),
                record.getDecisionTimestamp());
    }
}
