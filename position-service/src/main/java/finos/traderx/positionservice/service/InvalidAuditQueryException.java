package finos.traderx.positionservice.service;

/**
 * A query the caller got wrong, carrying a message written for the caller.
 *
 * Distinct from {@link IllegalArgumentException} so that the controller can answer with the message
 * verbatim: anything thrown below this service, by the persistence provider or elsewhere, carries
 * query and mapping detail that this endpoint must not hand back.
 */
public class InvalidAuditQueryException extends RuntimeException {

    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
