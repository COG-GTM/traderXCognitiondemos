package finos.traderx.tradeprocessor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTradeOrderException extends RuntimeException {
	public InvalidTradeOrderException(String message) {
		super(message);
	}
}
