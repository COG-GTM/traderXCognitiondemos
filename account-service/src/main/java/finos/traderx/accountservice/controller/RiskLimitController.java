package finos.traderx.accountservice.controller;

import java.util.List;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.exceptions.RiskLimitsDisabledException;
import finos.traderx.accountservice.model.RiskLimitHistory;
import finos.traderx.accountservice.model.RiskLimitRequest;
import finos.traderx.accountservice.model.RiskLimitView;
import finos.traderx.accountservice.service.RiskLimitService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "/account", produces = "application/json")
public class RiskLimitController {

	@Autowired
	RiskLimitService riskLimitService;

	/** Read path, called by trade-service on every order submission. */
	@GetMapping("/{id}/risk-limit")
	public ResponseEntity<RiskLimitView> getRiskLimit(@PathVariable int id) {
		return ResponseEntity.ok(this.riskLimitService.getRiskLimit(id));
	}

	/** Write path, called by the risk function only. */
	@PutMapping("/{id}/risk-limit")
	public ResponseEntity<RiskLimitView> setRiskLimit(@PathVariable int id, @RequestBody RiskLimitRequest request) {
		return ResponseEntity.ok(this.riskLimitService.setRiskLimit(id, request));
	}

	/** Evidence trail: every value that has been in force, most recent first. */
	@GetMapping("/{id}/risk-limit/history")
	public ResponseEntity<List<RiskLimitHistory>> getRiskLimitHistory(@PathVariable int id) {
		return ResponseEntity.ok(this.riskLimitService.getRiskLimitHistory(id));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<String> resourceNotFoundExceptionMapper(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> badRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	}

	/** An unparseable body is a bad limit payload, not a service failure. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<String> unreadableBody(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Risk limit payload could not be read: " + e.getMessage());
	}

	@ExceptionHandler(RiskLimitsDisabledException.class)
	public ResponseEntity<String> disabled(RiskLimitsDisabledException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> generalError(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	}
}
