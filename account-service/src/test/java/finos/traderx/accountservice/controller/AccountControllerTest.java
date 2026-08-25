package finos.traderx.accountservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.service.AccountService;

/** Edge and corner case coverage for the /account endpoints. */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AccountService accountService;

	private static Account account(int id, String displayName) {
		Account account = new Account();
		account.setId(id);
		account.setDisplayName(displayName);
		return account;
	}

	private MvcResult postAccount(String body) throws Exception {
		return this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/account/").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn();
	}

	// -------------------------------------------------------------- GET by id

	@Test
	@DisplayName("AS-16: GET /account/{id} for a missing id returns 404 with the exception message as the body")
	void missingAccountReturns404() throws Exception {
		when(this.accountService.getAccountById(42))
				.thenThrow(new ResourceNotFoundException("Account with id 42not found"));

		MvcResult result = this.mockMvc.perform(get("/account/42")).andReturn();

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals("Account with id 42not found", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-17a: GET /account/{id} with a non-numeric id returns 500 (not 400)")
	void nonNumericIdReturns500() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/account/abc")).andReturn();

		// The @ExceptionHandler(Exception.class) catch-all swallows the
		// MethodArgumentTypeMismatchException that would normally be a 400.
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("abc"),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: a malformed path variable is reported as 500 because AccountController's "
			+ "@ExceptionHandler(Exception.class) also catches MethodArgumentTypeMismatchException")
	@DisplayName("AS-17b: GET /account/{id} with a non-numeric id should return 400")
	void nonNumericIdShouldReturn400() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				this.mockMvc.perform(get("/account/abc")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-17c: GET /account/{id} with a non-numeric id leaks the internal exception message")
	void nonNumericIdLeaksInternals() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/account/abc")).andReturn();

		String body = result.getResponse().getContentAsString();
		assertTrue(body.contains("java.lang.Integer") || body.contains("NumberFormatException")
				|| body.contains("Failed to convert"), "body did not look like an internal message: " + body);
	}

	@Test
	@DisplayName("AS-17d: GET /account/-1 is passed straight through to the service")
	void negativeIdIsAccepted() throws Exception {
		when(this.accountService.getAccountById(-1)).thenReturn(account(-1, "Negative"));

		MvcResult result = this.mockMvc.perform(get("/account/-1")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("\"id\":-1"));
	}

	@Test
	@DisplayName("AS-17e: GET /account/0 is passed straight through to the service")
	void zeroIdIsAccepted() throws Exception {
		when(this.accountService.getAccountById(0)).thenReturn(account(0, "Zero"));

		assertEquals(HttpStatus.OK.value(),
				this.mockMvc.perform(get("/account/0")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-17f: GET /account/{id} with a value above Integer.MAX_VALUE returns 500")
	void overflowIdReturns500() throws Exception {
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				this.mockMvc.perform(get("/account/2147483648")).andReturn().getResponse().getStatus());
	}

	// ----------------------------------------------------------------- create

	@Test
	@DisplayName("AS-18a: POST /account/ accepts a displayName longer than the 50 char DB column")
	void overlongDisplayNameIsAccepted() throws Exception {
		String longName = "N".repeat(120);
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postAccount("{\"displayName\":\"" + longName + "\"}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		org.mockito.Mockito.verify(this.accountService).upsertAccount(captor.capture());
		assertEquals(120, captor.getValue().getDisplayName().length());
	}

	@Test
	@Disabled("LATENT BUG: displayName is never length-checked against the 50 char DisplayName column, "
			+ "so an overlong name is only rejected by the database at flush time")
	@DisplayName("AS-18b: POST /account/ should reject a displayName longer than 50 chars")
	void overlongDisplayNameShouldBeRejected() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postAccount("{\"displayName\":\"" + "N".repeat(120) + "\"}").getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-18c: POST /account/ accepts an empty displayName")
	void emptyDisplayNameIsAccepted() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		assertEquals(HttpStatus.OK.value(), postAccount("{\"displayName\":\"\"}").getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-18d: POST /account/ accepts a null displayName")
	void nullDisplayNameIsAccepted() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postAccount("{\"displayName\":null}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		org.mockito.Mockito.verify(this.accountService).upsertAccount(captor.capture());
		assertEquals(null, captor.getValue().getDisplayName());
	}

	@Test
	@DisplayName("AS-18e: POST /account/ accepts a unicode displayName and round-trips it")
	void unicodeDisplayNameIsAccepted() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postAccount("{\"displayName\":\"\u00c9quipe \u4ea4\u6613\"}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		result.getResponse().setCharacterEncoding("UTF-8");
		assertTrue(result.getResponse().getContentAsString().contains("\u00c9quipe"));
	}

	@Test
	@DisplayName("AS-19: POST /account/ with an explicit, already-used id is accepted (overwrite path)")
	void explicitIdIsAccepted() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postAccount("{\"id\":1,\"displayName\":\"Hijacked\"}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		org.mockito.Mockito.verify(this.accountService).upsertAccount(captor.capture());
		// The caller chooses the primary key; AccountService.upsertAccount then calls
		// save() which overwrites whatever account already owns that id.
		assertEquals(1, captor.getValue().getId());
	}

	@Test
	@Disabled("LATENT BUG: POST /account/ accepts a client-supplied id and AccountService.upsertAccount "
			+ "save()s over the existing row, so any caller can rename another account")
	@DisplayName("AS-19b: POST /account/ with an id that already exists should be rejected with 409")
	void existingIdShouldConflict() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		assertEquals(HttpStatus.CONFLICT.value(), postAccount("{\"id\":1,\"displayName\":\"Hijacked\"}")
				.getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-20: PUT /account/ for an id that does not exist is accepted (upsert, never 404)")
	void putUnknownIdIsAccepted() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = this.mockMvc.perform(put("/account/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":999999,\"displayName\":\"Ghost\"}"))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("999999"));
	}

	@Test
	@Disabled("LATENT BUG: PUT /account/ delegates to save() without an existence check, so updating a "
			+ "non-existent account silently creates it instead of returning 404")
	@DisplayName("AS-20b: PUT /account/ for an unknown id should return 404")
	void putUnknownIdShouldReturn404() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		assertEquals(HttpStatus.NOT_FOUND.value(), this.mockMvc.perform(put("/account/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":999999,\"displayName\":\"Ghost\"}"))
				.andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-18f: POST /account/ with a malformed body returns 500, not 400")
	void malformedBodyReturns500() throws Exception {
		MvcResult result = postAccount("{\"displayName\":");

		// HttpMessageNotReadableException is a client error, but the controller's
		// @ExceptionHandler(Exception.class) turns every unmapped exception into a 500.
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
	}

	@Test
	@Disabled("LATENT BUG: AccountController's @ExceptionHandler(Exception.class) also catches "
			+ "HttpMessageNotReadableException, so a malformed request body is reported as 500 not 400")
	@DisplayName("AS-18g: POST /account/ with a malformed body should return 400")
	void malformedBodyShouldReturn400() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(), postAccount("{\"displayName\":").getResponse().getStatus());
	}

	// ------------------------------------------------------------------- list

	@Test
	@DisplayName("AS-21: GET /account/ on an empty repository returns an empty JSON array, not null")
	void emptyListIsEmptyArray() throws Exception {
		when(this.accountService.getAllAccount()).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/account/")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-21b: GET /account/ serialises every account returned by the service")
	void listReturnsAllAccounts() throws Exception {
		when(this.accountService.getAllAccount()).thenReturn(List.of(account(1, "One"), account(2, "Two")));

		MvcResult result = this.mockMvc.perform(get("/account/")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		String body = result.getResponse().getContentAsString();
		assertTrue(body.contains("One") && body.contains("Two"), "body: " + body);
	}

	@Test
	@DisplayName("AS-27a: a repository/service failure is surfaced as a 500 whose body is the raw message")
	void serviceFailureLeaksMessage() throws Exception {
		when(this.accountService.getAccountById(anyInt()))
				.thenThrow(new IllegalStateException("jdbc:h2:tcp://db:18082/traderx connection refused"));

		MvcResult result = this.mockMvc.perform(get("/account/1")).andReturn();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("jdbc:h2:tcp://db:18082/traderx"),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: AccountController.generalError returns e.getMessage() to the client, leaking "
			+ "internal detail (connection strings, SQL, stack context) to unauthenticated callers")
	@DisplayName("AS-27b: a server error should not echo the internal exception message")
	void serviceFailureShouldNotLeakMessage() throws Exception {
		when(this.accountService.getAccountById(anyInt()))
				.thenThrow(new IllegalStateException("jdbc:h2:tcp://db:18082/traderx connection refused"));

		MvcResult result = this.mockMvc.perform(get("/account/1")).andReturn();

		assertTrue(!result.getResponse().getContentAsString().contains("jdbc:h2"),
				"internal detail leaked: " + result.getResponse().getContentAsString());
	}
}
