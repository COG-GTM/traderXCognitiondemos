package finos.traderx.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

/**
 * Layer C — in-process unit tests of {@code AccountUserController.validatePerson()} /
 * account-user creation. No sockets, no Spring context, no database: the controller is
 * instantiated directly, its {@link AccountUserService} collaborator is mocked, and the
 * {@link RestTemplate} bean is intercepted with {@link MockRestServiceServer}.
 *
 * Covers S1 (success), S2 (404 not found), S3 (400 bad request) and S7 (malformed / empty body),
 * and asserts the exact people-service URL that account-service builds.
 */
class AccountUserControllerMockRestTest {

	private static final String PEOPLE_SERVICE_URL = "http://people-service:18089";
	private static final String EXPECTED_URL = PEOPLE_SERVICE_URL + "/People/GetPerson?LogonId=jdoe";

	private MockRestServiceServer server;
	private AccountUserService accountUserService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		this.server = MockRestServiceServer.bindTo(restTemplate).build();
		this.accountUserService = Mockito.mock(AccountUserService.class);

		AccountUserController controller = new AccountUserController(restTemplate);
		ReflectionTestUtils.setField(controller, "accountUserService", accountUserService);
		ReflectionTestUtils.setField(controller, "peopleServiceAddress", PEOPLE_SERVICE_URL);

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	private String accountUserJson() {
		return "{\"accountId\":1,\"username\":\"jdoe\"}";
	}

	@Test
	void s1_personFound_createsAccountUser_andCallsExactUrl() throws Exception {
		when(accountUserService.upsertAccountUser(any(AccountUser.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		server.expect(requestTo(EXPECTED_URL))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(
						"{\"logonId\":\"jdoe\",\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\"}",
						MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountUserJson()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("jdoe")));

		server.verify();
	}

	@Test
	void s2_notFound_returns404() throws Exception {
		server.expect(requestTo(EXPECTED_URL))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountUserJson()))
				.andExpect(status().isNotFound())
				.andExpect(content().string("jdoe not found in People service."));

		server.verify();
	}

	@Test
	void s3_badRequest_treatedAsNotFound_returns404() throws Exception {
		server.expect(requestTo(EXPECTED_URL))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST));

		// FINDING: account-service conflates a 400 (bad request) from people-service with
		// "person not found" and degrades to a 404. Documented, not fixed, by this test.
		mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountUserJson()))
				.andExpect(status().isNotFound())
				.andExpect(content().string("jdoe not found in People service."));

		server.verify();
	}

	@Test
	void s7_malformedBody_returns500() throws Exception {
		server.expect(requestTo(EXPECTED_URL))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("<<not json>>", MediaType.APPLICATION_JSON));

		// FINDING: a non-JSON 200 body raises RestClientException, which is not caught by the
		// HttpClientErrorException branch, so account-service degrades to a blunt 500.
		mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountUserJson()))
				.andExpect(status().isInternalServerError());

		server.verify();
	}

	@Test
	void s7_emptyBody_returns500() throws Exception {
		server.expect(requestTo(EXPECTED_URL))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(""));

		// FINDING: an empty 200 body yields a null Person, and the success path calls
		// response.getBody().toString() -> NullPointerException -> generic 500.
		mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountUserJson()))
				.andExpect(status().isInternalServerError());

		server.verify();
	}
}
