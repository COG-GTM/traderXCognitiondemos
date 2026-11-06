package finos.traderx.accountservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountUserConflictException extends RuntimeException {
	public AccountUserConflictException(String message) {
		super(message);
	}
}
