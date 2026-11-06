package finos.traderx.positionservice.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Error responses are always JSON with a fixed message so that request input and
 * internal details (exception messages, database hosts) are never echoed back.
 */
@RestControllerAdvice(assignableTypes = { PositionController.class, TradeController.class })
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> invalidArgument(MethodArgumentTypeMismatchException e) {
		log.debug("Invalid value for request parameter '{}'", e.getName());
		return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> generalError(Exception e) {
		log.error("Unhandled error while processing request", e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("error", message));
	}
}
