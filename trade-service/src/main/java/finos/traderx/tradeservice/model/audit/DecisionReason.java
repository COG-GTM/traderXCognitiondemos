package finos.traderx.tradeservice.model.audit;

/**
 * Machine readable reason attached to every order decision. Values are part of the
 * retained regulatory record and must not be renamed once written.
 */
public enum DecisionReason {
    VALIDATED,
    SECURITY_NOT_FOUND,
    ACCOUNT_NOT_FOUND
}
