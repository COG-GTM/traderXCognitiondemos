package finos.traderx.tradeservice.model;

import java.math.BigDecimal;

/**
 * Outcome of the pre-trade risk evaluation of a single order. Serialised as the
 * response body when an order is rejected.
 */
public class RiskDecision {

    public static final String REASON_WITHIN_LIMIT = "WITHIN_LIMIT";
    public static final String REASON_CHECKS_DISABLED = "PRE_TRADE_CHECKS_DISABLED";
    public static final String REASON_NOTIONAL_LIMIT_BREACH = "NOTIONAL_LIMIT_BREACH";
    public static final String REASON_PRICE_UNAVAILABLE = "PRICE_UNAVAILABLE";

    private final String decision;
    private final String reason;
    private final BigDecimal limit;
    private final BigDecimal attempted;

    private RiskDecision(String decision, String reason, BigDecimal limit, BigDecimal attempted) {
        this.decision = decision;
        this.reason = reason;
        this.limit = limit;
        this.attempted = attempted;
    }

    public static RiskDecision accepted(String reason, BigDecimal limit, BigDecimal attempted) {
        return new RiskDecision("ACCEPTED", reason, limit, attempted);
    }

    public static RiskDecision rejected(String reason, BigDecimal limit, BigDecimal attempted) {
        return new RiskDecision("REJECTED", reason, limit, attempted);
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public BigDecimal getAttempted() {
        return attempted;
    }

    public boolean isRejected() {
        return "REJECTED".equals(decision);
    }

    @Override
    public String toString() {
        return "RiskDecision{decision=" + decision + ", reason=" + reason
                + ", limit=" + limit + ", attempted=" + attempted + "}";
    }
}
