package finos.traderx.tradeprocessor.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTradeIdException extends RuntimeException {
	public DuplicateTradeIdException(String message) {
		super(message);
	}
}
