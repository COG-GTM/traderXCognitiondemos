package finos.traderx.accountservice.controller;

import java.util.List;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.model.Person;
import finos.traderx.accountservice.service.AccountUserService;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/accountuser", produces="application/json")
public class AccountUserController {

	private static final Logger log = LoggerFactory.getLogger(AccountUserController.class);

	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	AccountUserService accountUserService;

	@Autowired
	private Tracer tracer;

	@Value("${people.service.url}")
	private String peopleServiceAddress;

	@GetMapping("/{id}")
	@WithSpan("accountUser.getById")
	public ResponseEntity<AccountUser> getAccountUserById(@SpanAttribute("accountUser.id") @PathVariable int id) {
		log.info("Getting account user by id={}", id);
		Span span = Span.current();
		try {
			AccountUser retVal = this.accountUserService.getAccountUserById(id);
			span.setAttribute("accountUser.username", retVal.getUsername());
			span.setAttribute("accountUser.accountId", retVal.getAccountId());
			span.setStatus(StatusCode.OK);
			log.info("Account user found: id={}, username={}", id, retVal.getUsername());
			return ResponseEntity.ok(retVal);
		} catch (ResourceNotFoundException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR, "Account user not found");
			throw e;
		}
	}

	@PostMapping("/")
	@WithSpan("accountUser.create")
	public ResponseEntity<AccountUser> createAccountUser(@RequestBody AccountUser accountUser) {
		Span span = Span.current();
		span.setAttribute("accountUser.username", accountUser.getUsername());
		span.setAttribute("accountUser.accountId", accountUser.getAccountId());
		log.info("Creating account user: username={}, accountId={}", accountUser.getUsername(), accountUser.getAccountId());

		if (validatePerson(accountUser.getUsername())) {
			span.addEvent("Person validated");
			AccountUser created = this.accountUserService.upsertAccountUser(accountUser);
			span.setStatus(StatusCode.OK);
			span.addEvent("Account user created");
			log.info("Account user created: username={}, accountId={}", created.getUsername(), created.getAccountId());
			return ResponseEntity.ok(created);
		}
		else {
			span.setStatus(StatusCode.ERROR, "Person not found");
			span.setAttribute("accountUser.validation.result", "person_not_found");
			throw new ResourceNotFoundException(accountUser.getUsername() + " not found in People service.");
		}
	}

	@PutMapping("/")
	@WithSpan("accountUser.update")
	public ResponseEntity<AccountUser> updateAccountUser(@RequestBody AccountUser accountUser) {
		Span span = Span.current();
		span.setAttribute("accountUser.username", accountUser.getUsername());
		span.setAttribute("accountUser.accountId", accountUser.getAccountId());
		log.info("Updating account user: username={}, accountId={}", accountUser.getUsername(), accountUser.getAccountId());
		AccountUser updated = this.accountUserService.upsertAccountUser(accountUser);
		span.setStatus(StatusCode.OK);
		span.addEvent("Account user updated");
		log.info("Account user updated: username={}, accountId={}", updated.getUsername(), updated.getAccountId());
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/")
	@WithSpan("accountUser.getAll")
	public ResponseEntity<List<AccountUser>> getAllAccountUsers() {
		log.info("Getting all account users");
		Span span = Span.current();
		List<AccountUser> users = this.accountUserService.getAllAccountUsers();
		span.setAttribute("accountUser.count", users.size());
		span.setStatus(StatusCode.OK);
		log.info("Retrieved {} account users", users.size());
		return ResponseEntity.ok(users);
	}

	private boolean validatePerson(String username) {
		Span span = tracer.spanBuilder("accountUser.validatePerson")
				.setParent(Context.current())
				.startSpan();
		try (Scope scope = span.makeCurrent()) {
			span.setAttribute("person.username", username);
			String url = this.peopleServiceAddress + "/People/GetPerson" + "?LogonId=" + username;
			ResponseEntity<Person> response = null;

			try {
				response = this.restTemplate.getForEntity(url, Person.class);
				log.info("Validated person {}", response.getBody().toString());
				span.setStatus(StatusCode.OK);
				span.setAttribute("person.valid", true);
				return true;
			}
			catch (HttpClientErrorException ex) {
				if (ex.getRawStatusCode() == 404) {
					log.info("{} not found in People service.", username);
					span.setAttribute("person.valid", false);
					span.setStatus(StatusCode.ERROR, "Person not found");
				}
				else {
					log.error(ex.getMessage());
					span.recordException(ex);
					span.setStatus(StatusCode.ERROR, ex.getMessage());
				}
				return false;
			}
		} finally {
			span.end();
		}
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
