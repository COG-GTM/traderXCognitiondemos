package finos.traderx.accountservice.controller;

import java.util.List;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.service.AccountService;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin("*")
@RestController
@RequestMapping(value="/account", produces="application/json")
public class AccountController {

	private static final Logger log = LoggerFactory.getLogger(AccountController.class);

	@Autowired
	AccountService accountService;

	@GetMapping("/{id}")
	@WithSpan("account.getById")
	public ResponseEntity<Account> getAccountById(@SpanAttribute("account.id") @PathVariable int id) {
		log.info("Getting account by id={}", id);
		Span span = Span.current();
		try {
			Account retVal = this.accountService.getAccountById(id);
			span.setAttribute("account.displayName", retVal.getDisplayName());
			span.setStatus(StatusCode.OK);
			log.info("Account found: id={}, displayName={}", id, retVal.getDisplayName());
			return ResponseEntity.ok(retVal);
		} catch (ResourceNotFoundException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR, "Account not found");
			throw e;
		}
	}

	@PostMapping("/")
	@WithSpan("account.create")
	public ResponseEntity<Account> createAccount(@RequestBody Account account) {
		Span span = Span.current();
		span.setAttribute("account.displayName", account.getDisplayName());
		log.info("Creating account: displayName={}", account.getDisplayName());
		Account created = this.accountService.upsertAccount(account);
		span.setAttribute("account.id", created.getId());
		span.setStatus(StatusCode.OK);
		span.addEvent("Account created");
		log.info("Account created: id={}, displayName={}", created.getId(), created.getDisplayName());
		return ResponseEntity.ok(created);
	}

	@PutMapping("/")
	@WithSpan("account.update")
	public ResponseEntity<Account> updateAccount(@RequestBody Account account) {
		Span span = Span.current();
		span.setAttribute("account.id", account.getId());
		span.setAttribute("account.displayName", account.getDisplayName());
		log.info("Updating account: id={}, displayName={}", account.getId(), account.getDisplayName());
		Account updated = this.accountService.upsertAccount(account);
		span.setStatus(StatusCode.OK);
		span.addEvent("Account updated");
		log.info("Account updated successfully: id={}", updated.getId());
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/")
	@WithSpan("account.getAll")
	public ResponseEntity<List<Account>> getAllAccount() {
		log.info("Getting all accounts");
		Span span = Span.current();
		List<Account> accounts = this.accountService.getAllAccount();
		span.setAttribute("account.count", accounts.size());
		span.setStatus(StatusCode.OK);
		log.info("Retrieved {} accounts", accounts.size());
		return ResponseEntity.ok(accounts);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<String> resourceNotFoundExceptionMapper(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> generalError(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	}
}
