package finos.traderx.tradeservice.regulatory;

import java.util.function.Predicate;

import finos.traderx.tradeservice.model.TradeOrder;

/**
 * One row of the regulatory rule table. A rule breaches when {@code compliant} returns false.
 */
public record RegulatoryRule(RejectionCode code, String field, String description,
        Predicate<TradeOrder> compliant) {

    public boolean isBreachedBy(TradeOrder order) {
        return !this.compliant.test(order);
    }

    public RejectionReason toRejection() {
        return new RejectionReason(this.code, this.field, this.description);
    }
}
