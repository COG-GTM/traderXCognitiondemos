package finos.traderx.accountservice.exceptions;

import org.springframework.http.HttpStatus;

public class AdminAccessDeniedException extends RuntimeException {

	private final HttpStatus status;

	public AdminAccessDeniedException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return this.status;
	}
}
