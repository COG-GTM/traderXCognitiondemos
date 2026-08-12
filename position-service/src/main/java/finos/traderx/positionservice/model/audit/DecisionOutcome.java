package finos.traderx.positionservice.model.audit;

/**
 * Mirrors the values written by trade-service (TRX-104). Persisted as a string in the
 * retained record, so the names are part of the regulatory record and must not be renamed.
 */
public enum DecisionOutcome {
    ACCEPTED,
    REJECTED
}
