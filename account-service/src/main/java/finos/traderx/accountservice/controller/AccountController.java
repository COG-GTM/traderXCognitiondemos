package finos.traderx.accountservice.controller;

import java.util.List;

import finos.traderx.accountservice.dto.AccountRequest;
import finos.traderx.accountservice.exceptions.InvalidRequestException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.service.AccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value="/account", produces="application/json")
public class AccountController {

	@Autowired
	AccountService accountService;

	@GetMapping("/{id}")
	public ResponseEntity<Account> getAccountById(@PathVariable int id) {
		Account retVal = this.accountService.getAccountById(id);
		return ResponseEntity.ok(retVal);
	}

	@PostMapping("/")
	public ResponseEntity<Account> createAccount(@RequestBody AccountRequest account) {
		return ResponseEntity.ok(this.accountService.createAccount(account.getDisplayName()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Account> updateAccount(@PathVariable int id, @RequestBody AccountRequest account) {
		return ResponseEntity.ok(this.accountService.updateAccount(id, account.getDisplayName()));
	}

	@GetMapping("/")
	public ResponseEntity<List<Account>> getAllAccount() {
		return
				(ResponseEntity<List<Account>>) ResponseEntity.ok(this.accountService.getAllAccount());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<String> resourceNotFoundExceptionMapper(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<String> invalidRequestExceptionMapper(InvalidRequestException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> generalError(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	}
}
