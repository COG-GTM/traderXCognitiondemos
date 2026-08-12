package finos.traderx.tradeservice.model.audit;

/**
 * Machine readable reason attached to every order decision. Values are part of the
 * retained regulatory record and must not be renamed once written.
 */
public enum DecisionReason {
    VALIDATED,
    SECURITY_NOT_FOUND,
    ACCOUNT_NOT_FOUND,
    /** A downstream check could not be performed, so the order was refused without being validated. */
    VALIDATION_UNAVAILABLE,
    /** A downstream check rejected the request itself, so the submission was malformed. */
    SUBMISSION_INVALID,
    /**
     * The order was validated, but could not be handed to the trade feed. Appended after the
     * ACCEPTED record under the same correlation id, since neither record can be amended.
     */
    DISPATCH_FAILED
}
