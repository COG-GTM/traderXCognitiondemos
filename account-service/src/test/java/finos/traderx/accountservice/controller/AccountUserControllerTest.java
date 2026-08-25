package finos.traderx.accountservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountUserService;

/**
 * Edge and corner case coverage for the /accountuser endpoints, including the
 * outbound people-service validation call (served by {@link MockRestServiceServer}).
 */
@WebMvcTest(AccountUserController.class)
@TestPropertySource(properties = "people.service.url=http://people-service:18089")
class AccountUserControllerTest {

	private static final String PEOPLE_SERVICE = "http://people-service:18089";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountUserController controller;

	@MockBean
	private AccountUserService accountUserService;

	private MockRestServiceServer mockServer;

	private final List<String> observedUris = new ArrayList<>();

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		this.observedUris.clear();
		ReflectionTestUtils.setField(this.controller, "restTemplate", restTemplate);
	}

	private static AccountUser accountUser(int accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

	private void expectPeopleLookup(org.springframework.test.web.client.ResponseCreator response) {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(response);
	}

	private MvcResult postUser(String body) throws Exception {
		return this.mockMvc.perform(post("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn();
	}

	private static String person() {
		return "{\"logonId\":\"bob\",\"fullName\":\"Bob\",\"email\":\"bob@example.com\"}";
	}

	// ------------------------------------------------------------- happy path

	@Test
	@DisplayName("AS-22a: POST /accountuser/ validates the username against people-service and saves")
	void validPersonIsSaved() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob\"}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=bob", this.observedUris.get(0));
		verify(this.accountUserService).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	@DisplayName("AS-22b: people-service 404 yields 404 and nothing is saved")
	void unknownPersonReturns404() throws Exception {
		expectPeopleLookup(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"nobody\"}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals("nobody not found in People service.", result.getResponse().getContentAsString());
		verify(this.accountUserService, never()).upsertAccountUser(any());
	}

	@Test
	@DisplayName("AS-22c: people-service 500 is caught by the catch-all handler and returned as 500")
	void peopleServiceErrorReturns500() throws Exception {
		expectPeopleLookup(withServerError());

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob\"}");

		// HttpServerErrorException is NOT handled by validatePerson (it only catches
		// HttpClientErrorException); it bubbles up to @ExceptionHandler(Exception.class).
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("500"),
				"body: " + result.getResponse().getContentAsString());
		verify(this.accountUserService, never()).upsertAccountUser(any());
	}

	@Test
	@DisplayName("AS-22d: people-service unreachable yields a 500 whose body leaks the target URL")
	void peopleServiceUnreachableReturns500() throws Exception {
		expectPeopleLookup(request -> {
			throw new ResourceAccessException("I/O error on GET request for \""
					+ PEOPLE_SERVICE + "/People/GetPerson\": Connection refused");
		});

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob\"}");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains(PEOPLE_SERVICE),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: an unavailable people-service surfaces as a 500 whose body echoes the internal "
			+ "people-service URL; it should be a 503 with no internal detail")
	@DisplayName("AS-22e: people-service unavailability should map to 503 without leaking internals")
	void peopleServiceUnreachableShouldReturn503() throws Exception {
		expectPeopleLookup(request -> {
			throw new ResourceAccessException("Connection refused to " + PEOPLE_SERVICE);
		});

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob\"}");

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), result.getResponse().getStatus());
		assertTrue(!result.getResponse().getContentAsString().contains(PEOPLE_SERVICE));
	}

	// -------------------------------------------------- query param injection

	@Test
	@DisplayName("AS-23a: a username containing '&' injects an extra query parameter into the people-service call")
	void usernameWithAmpersandInjectsQueryParameter() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob&IsAdmin=true\"}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		// The URL is built by raw concatenation ("?LogonId=" + username), so the caller
		// controls the outbound query string.
		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=bob&IsAdmin=true", this.observedUris.get(0));
	}

	@Test
	@Disabled("LATENT BUG: AccountUserController.validatePerson concatenates the username into the "
			+ "people-service query string unencoded, allowing query-parameter injection")
	@DisplayName("AS-23b: a username containing '&' should be percent-encoded, not injected")
	void usernameWithAmpersandShouldBeEncoded() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		postUser("{\"accountId\":1,\"username\":\"bob&IsAdmin=true\"}");

		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=bob%26IsAdmin%3Dtrue", this.observedUris.get(0));
	}

	@Test
	@DisplayName("AS-23c: a username containing spaces is encoded as %20 in the outbound URL")
	void usernameWithSpacesIsEncoded() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		postUser("{\"accountId\":1,\"username\":\"bob smith\"}");

		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=bob%20smith", this.observedUris.get(0));
	}

	@Test
	@DisplayName("AS-23d: a unicode username is percent-encoded in the outbound URL")
	void unicodeUsernameIsEncoded() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		postUser("{\"accountId\":1,\"username\":\"\u00e9lodie\"}");

		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=%C3%A9lodie", this.observedUris.get(0));
	}

	@Test
	@DisplayName("AS-23e: an empty username is still sent to people-service as an empty LogonId")
	void emptyUsernameIsSent() throws Exception {
		expectPeopleLookup(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"\"}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=", this.observedUris.get(0));
	}

	// -------------------------------------------------------- PUT asymmetry

	@Test
	@DisplayName("AS-24a: PUT /accountuser/ saves without validating the username against people-service")
	void putSkipsPersonValidation() throws Exception {
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		MvcResult result = this.mockMvc.perform(put("/accountuser/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":1,\"username\":\"does-not-exist\"}"))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		verify(this.accountUserService).upsertAccountUser(any(AccountUser.class));
		// No outbound people-service call was made at all.
		this.mockServer.verify();
		assertTrue(this.observedUris.isEmpty(), "unexpected outbound calls: " + this.observedUris);
	}

	@Test
	@Disabled("LATENT BUG: PUT /accountuser/ performs no people-service validation while POST does, so an "
			+ "arbitrary username can be attached to an account through the update path")
	@DisplayName("AS-24b: PUT /accountuser/ should validate the person like POST does")
	void putShouldValidatePerson() throws Exception {
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));

		this.mockMvc.perform(put("/accountuser/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":1,\"username\":\"does-not-exist\"}"))
				.andReturn();

		assertEquals(1, this.observedUris.size(), "people-service was never consulted");
	}

	// ------------------------------------------------------------- read paths

	@Test
	@DisplayName("AS-24c: GET /accountuser/{id} for a missing id returns 404 with the message body")
	void missingAccountUserReturns404() throws Exception {
		when(this.accountUserService.getAccountUserById(7))
				.thenThrow(new ResourceNotFoundException("AccountUser with id 7not found"));

		MvcResult result = this.mockMvc.perform(get("/accountuser/7")).andReturn();

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals("AccountUser with id 7not found", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-24d: GET /accountuser/{id} with a non-numeric id returns 500 (not 400)")
	void nonNumericAccountUserIdReturns500() throws Exception {
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				this.mockMvc.perform(get("/accountuser/abc")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("AS-24e: GET /accountuser/ on an empty repository returns an empty array")
	void emptyAccountUserListIsEmptyArray() throws Exception {
		when(this.accountUserService.getAllAccountUsers()).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/accountuser/")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-24f: POST /accountuser/ for an unknown account propagates the service's 404")
	void unknownAccountOnCreateReturns404() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class)))
				.thenThrow(new ResourceNotFoundException("Account with id 55not found"));

		MvcResult result = postUser("{\"accountId\":55,\"username\":\"bob\"}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals("Account with id 55not found", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-24g: POST /accountuser/ with a null username sends the literal 'null' as the LogonId")
	void nullUsernameIsSentAsLiteralNull() throws Exception {
		expectPeopleLookup(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postUser("{\"accountId\":1,\"username\":null}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(PEOPLE_SERVICE + "/People/GetPerson?LogonId=null", this.observedUris.get(0));
		verify(this.accountUserService, never()).upsertAccountUser(any());
		assertEquals("null not found in People service.", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("AS-24h: an AccountUser payload round-trips both composite key parts")
	void accountUserRoundTrips() throws Exception {
		expectPeopleLookup(withSuccess(person(), MediaType.APPLICATION_JSON));
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class)))
				.thenReturn(accountUser(1, "bob"));

		MvcResult result = postUser("{\"accountId\":1,\"username\":\"bob\"}");

		String body = result.getResponse().getContentAsString();
		assertTrue(body.contains("\"accountId\":1") && body.contains("\"username\":\"bob\""), "body: " + body);
	}
}
