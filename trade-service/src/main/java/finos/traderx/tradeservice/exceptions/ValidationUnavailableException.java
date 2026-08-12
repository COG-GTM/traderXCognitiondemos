package finos.traderx.tradeservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A downstream check could not be performed, so the order was refused without being validated.
 * Distinct from {@link ResourceNotFoundException}: the order is not known to be invalid, we
 * were unable to establish whether it is.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ValidationUnavailableException extends RuntimeException {

    public ValidationUnavailableException(String message) {
        super(message);
    }
}
