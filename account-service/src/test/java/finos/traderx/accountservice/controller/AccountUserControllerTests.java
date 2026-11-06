package finos.traderx.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(AccountUserController.class)
@TestPropertySource(properties = "people.service.url=http://people.test")
class AccountUserControllerTests {

	private static final String PEOPLE_LOOKUP = "http://people.test/People/GetPerson?LogonId=";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	AccountUserController controller;

	@MockBean
	AccountUserService accountUserService;

	MockRestServiceServer peopleService;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(this.controller, "restTemplate");
		this.peopleService = MockRestServiceServer.bindTo(restTemplate).build();
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void postWithKnownUsernameIsSaved() throws Exception {
		expectPerson("user01", "user01");

		this.mockMvc.perform(json(post("/accountuser/"), 22214, "user01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("user01"));

		this.peopleService.verify();
		verify(this.accountUserService).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void putWithKnownUsernameIsSaved() throws Exception {
		expectPerson("user01", "user01");

		this.mockMvc.perform(json(put("/accountuser/"), 22214, "user01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("user01"));

		this.peopleService.verify();
		verify(this.accountUserService).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void putWithUnknownUsernameIsRejected() throws Exception {
		this.peopleService.expect(requestTo(PEOPLE_LOOKUP + "ghost"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		this.mockMvc.perform(json(put("/accountuser/"), 22214, "ghost"))
				.andExpect(status().isNotFound());

		this.peopleService.verify();
		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void postWithUnknownUsernameIsRejected() throws Exception {
		this.peopleService.expect(requestTo(PEOPLE_LOOKUP + "ghost"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		this.mockMvc.perform(json(post("/accountuser/"), 22214, "ghost"))
				.andExpect(status().isNotFound());

		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void queryStringCharactersInUsernameAreRejectedBeforeLookup() throws Exception {
		for (String username : new String[] { "user01&x=1", "x&EmployeeId=E1", "&EmployeeId=E0001", "user01#x", "user01?x", "user01 x", "user01%26x" }) {
			this.mockMvc.perform(json(post("/accountuser/"), 22214, username))
					.andExpect(status().isBadRequest());
			this.mockMvc.perform(json(put("/accountuser/"), 22214, username))
					.andExpect(status().isBadRequest());
		}

		this.peopleService.verify();
		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void missingOrOverlongUsernameIsRejected() throws Exception {
		this.mockMvc.perform(post("/accountuser/").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":22214}"))
				.andExpect(status().isBadRequest());
		this.mockMvc.perform(json(put("/accountuser/"), 22214, "abcdefghijklmnop"))
				.andExpect(status().isBadRequest());

		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	void peopleServiceReturningDifferentLogonIdIsRejected() throws Exception {
		expectPerson("user01", "user02");

		this.mockMvc.perform(json(post("/accountuser/"), 22214, "user01"))
				.andExpect(status().isNotFound());

		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	private void expectPerson(String requested, String returnedLogonId) {
		this.peopleService.expect(requestTo(PEOPLE_LOOKUP + requested))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("{\"logonId\":\"" + returnedLogonId + "\",\"fullName\":\"Test User\"}",
						MediaType.APPLICATION_JSON));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, int accountId, String username) {
		return builder.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":" + accountId + ",\"username\":\"" + username + "\"}");
	}
}
