package finos.traderx.tradeservice.regulatory;

import java.util.List;

/**
 * Raised when a trade order breaches the regulatory rule set. Carries the machine readable
 * rejection reasons so the caller receives a reportable failure rather than a generic error.
 */
public class RegulatoryValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<RejectionReason> rejections;

    public RegulatoryValidationException(List<RejectionReason> rejections) {
        super("Trade order rejected by " + RegulatoryRuleSet.EMIR_REFIT + " validation: " + rejections);
        this.rejections = rejections;
    }

    public List<RejectionReason> getRejections() {
        return this.rejections;
    }
}
