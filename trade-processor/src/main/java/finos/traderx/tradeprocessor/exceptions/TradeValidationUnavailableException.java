package finos.traderx.tradeprocessor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class TradeValidationUnavailableException extends RuntimeException {
	public TradeValidationUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
