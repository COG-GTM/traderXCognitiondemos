package finos.traderx.accountservice.controller;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.exceptions.AdminAccessDeniedException;
import finos.traderx.accountservice.exceptions.InvalidAccountUserException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.model.Person;
import finos.traderx.accountservice.security.AccountAdminAuthorizer;
import finos.traderx.accountservice.service.AccountUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/accountuser", produces="application/json")
public class AccountUserController {

	private static final Logger logger = LoggerFactory.getLogger(AccountUserController.class);

	// Matches the AccountUsers.Username VARCHAR(15) column.
	static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,15}$");

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	AccountUserService accountUserService;

	@Autowired
	AccountAdminAuthorizer accountAdminAuthorizer;

	@Value("${people.service.url}")
	private String peopleServiceAddress;

	@GetMapping("/{id}")
	public ResponseEntity<AccountUser> getAccountUserById(@PathVariable int id) {
		AccountUser retVal = this.accountUserService.getAccountUserById(id);
		return ResponseEntity.ok(retVal);
	}

	@PostMapping("/")
	public ResponseEntity<AccountUser> createAccountUser(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody AccountUser accountUser) {
		this.accountAdminAuthorizer.requireAdmin(authorization);
		String username = validatePerson(accountUser);
		return ResponseEntity.ok(this.accountUserService.createAccountUser(accountUser.getAccountId(), username));
	}

	@PutMapping("/")
	public ResponseEntity<AccountUser> updateAccountUser(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody AccountUser accountUser) {
		this.accountAdminAuthorizer.requireAdmin(authorization);
		String username = validatePerson(accountUser);
		return ResponseEntity.ok(this.accountUserService.upsertAccountUser(accountUser.getAccountId(), username));
	}

	@GetMapping("/")
	public ResponseEntity<List<AccountUser>> getAllAccountUsers() {
		return ResponseEntity.ok(this.accountUserService.getAllAccountUsers());
	}

	/**
	 * Confirms the requested username is a known person and returns the People service's logonId for it.
	 */
	private String validatePerson(AccountUser accountUser) {
		if (accountUser == null || accountUser.getAccountId() == null) {
			throw new InvalidAccountUserException("accountId is required");
		}
		String username = accountUser.getUsername();
		if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
			throw new InvalidAccountUserException("username must be 1-15 characters of letters, digits, '.', '_' or '-'");
		}

		URI uri = UriComponentsBuilder.fromHttpUrl(this.peopleServiceAddress)
				.path("/People/GetPerson")
				.queryParam("LogonId", "{logonId}")
				.encode()
				.buildAndExpand(username)
				.toUri();

		Person person;
		try {
			person = this.restTemplate.getForObject(uri, Person.class);
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().value() == 404) {
				logger.info("{} not found in People service.", username);
			}
			else {
				logger.error("People service lookup failed: {}", ex.getMessage());
			}
			throw new ResourceNotFoundException(username + " not found in People service.");
		}

		if (person == null || person.getLogonId() == null || !person.getLogonId().equals(username)) {
			throw new ResourceNotFoundException(username + " not found in People service.");
		}
		logger.info("Validated person {}", person.getLogonId());
		return person.getLogonId();
	}

	@ExceptionHandler(AdminAccessDeniedException.class)
	public ResponseEntity<String> adminAccessDeniedMapper(AdminAccessDeniedException e) {
		ResponseEntity.BodyBuilder response = ResponseEntity.status(e.getStatus());
		if (e.getStatus() == HttpStatus.UNAUTHORIZED) {
			response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		}
		return response.body(e.getMessage());
	}

	@ExceptionHandler(InvalidAccountUserException.class)
	public ResponseEntity<String> invalidAccountUserMapper(InvalidAccountUserException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	}

	@ExceptionHandler(AccountUserConflictException.class)
	public ResponseEntity<String> accountUserConflictMapper(AccountUserConflictException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
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
