package finos.traderx.accountservice.controller;

import java.util.List;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountUserService;

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
@RequestMapping(value="/accountuser", produces="application/json")
public class AccountUserController {

	@Autowired
	AccountUserService accountUserService;

	@GetMapping("/{id}")
	public ResponseEntity<AccountUser> getAccountUserById(@PathVariable int id) {
		AccountUser retVal = this.accountUserService.getAccountUserById(id);
		return ResponseEntity.ok(retVal);
	}

	@PostMapping("/")
	public ResponseEntity<AccountUser> createAccountUser(@RequestBody AccountUser accountUser) {
		return ResponseEntity.ok(this.accountUserService.upsertAccountUser(accountUser));
	}

	@PutMapping("/")
	public ResponseEntity<AccountUser> updateAccountUser(@RequestBody AccountUser accountUser) {
		return ResponseEntity.ok(this.accountUserService.upsertAccountUser(accountUser));
	}

	@GetMapping("/")
	public ResponseEntity<List<AccountUser>> getAllAccountUsers() {
		return ResponseEntity.ok(this.accountUserService.getAllAccountUsers());
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
