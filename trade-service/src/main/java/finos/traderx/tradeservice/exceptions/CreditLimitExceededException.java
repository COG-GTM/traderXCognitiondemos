package finos.traderx.tradeservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class CreditLimitExceededException extends RuntimeException {
	public CreditLimitExceededException(String message) {
		super(message);
	}
}
