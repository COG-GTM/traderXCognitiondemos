package finos.traderx.tradeservice.model.audit;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Snapshot of the limit that was in force when a decision was taken. Copied into the audit
 * record by value so the decision stays reconstructable after the limit is later amended.
 */
public record EvaluatedLimit(String limitId, String limitType, BigDecimal limitValue, Instant effectiveFrom) {
}
