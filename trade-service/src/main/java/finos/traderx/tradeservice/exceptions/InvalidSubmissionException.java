package finos.traderx.tradeservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A lookup service understood the question and objected to it, so the submission itself is
 * malformed. Distinct from {@link ValidationUnavailableException}, which means we could not
 * ask at all — the audit record must not confuse a bad order with an outage.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSubmissionException extends RuntimeException {

    public InvalidSubmissionException(String message) {
        super(message);
    }
}
