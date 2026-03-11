package finos.traderx.validationservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.validationservice.model.ValidationRequest;
import finos.traderx.validationservice.model.ValidationResult;
import finos.traderx.validationservice.service.AccountValidationService;
import finos.traderx.validationservice.service.PersonValidationService;
import finos.traderx.validationservice.service.TickerValidationService;

import io.swagger.v3.oas.annotations.Operation;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "/validate", produces = "application/json")
public class ValidationController {

	private static final Logger log = LoggerFactory.getLogger(ValidationController.class);

	@Autowired
	private TickerValidationService tickerValidationService;

	@Autowired
	private AccountValidationService accountValidationService;

	@Autowired
	private PersonValidationService personValidationService;

	@Operation(description = "Validate a security ticker against the reference data service")
	@GetMapping("/ticker/{ticker}")
	public ResponseEntity<ValidationResult> validateTicker(@PathVariable String ticker) {
		log.info("Validating ticker: {}", ticker);
		ValidationResult result = new ValidationResult();
		if (!tickerValidationService.validateTicker(ticker)) {
			result.addError(ticker + " not found in Reference data service.");
		}
		return ResponseEntity.ok(result);
	}

	@Operation(description = "Validate an account ID against the account service")
	@GetMapping("/account/{id}")
	public ResponseEntity<ValidationResult> validateAccount(@PathVariable Integer id) {
		log.info("Validating account: {}", id);
		ValidationResult result = new ValidationResult();
		if (!accountValidationService.validateAccount(id)) {
			result.addError(id + " not found in Account service.");
		}
		return ResponseEntity.ok(result);
	}

	@Operation(description = "Validate a person/username against the people service")
	@GetMapping("/person")
	public ResponseEntity<ValidationResult> validatePerson(@RequestParam String username) {
		log.info("Validating person: {}", username);
		ValidationResult result = new ValidationResult();
		if (!personValidationService.validatePerson(username)) {
			result.addError(username + " not found in People service.");
		}
		return ResponseEntity.ok(result);
	}

	@Operation(description = "Validate a trade order (ticker + account)")
	@PostMapping("/trade-order")
	public ResponseEntity<ValidationResult> validateTradeOrder(@RequestBody ValidationRequest request) {
		log.info("Validating trade order: security={}, accountId={}", request.getSecurity(), request.getAccountId());
		ValidationResult result = new ValidationResult();

		if (request.getSecurity() != null && !tickerValidationService.validateTicker(request.getSecurity())) {
			result.addError(request.getSecurity() + " not found in Reference data service.");
		}

		if (request.getAccountId() != null && !accountValidationService.validateAccount(request.getAccountId())) {
			result.addError(request.getAccountId() + " not found in Account service.");
		}

		return ResponseEntity.ok(result);
	}
}
