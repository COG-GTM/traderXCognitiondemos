package finos.traderx.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.model.Person;
import finos.traderx.accountservice.service.AccountUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(AccountUserController.class)
@TestPropertySource(properties = "people.service.url=http://people-service:18089")
class AccountUserControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	AccountUserController accountUserController;

	@MockBean
	AccountUserService accountUserService;

	private RestTemplate restTemplate;

	@BeforeEach
	void replacePeopleServiceClient() {
		this.restTemplate = Mockito.mock(RestTemplate.class);
		ReflectionTestUtils.setField(this.accountUserController, "restTemplate", this.restTemplate);
	}

	private AccountUser accountUser(Integer accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

	private Person person(String logonId) {
		Person person = new Person();
		person.setLogonId(logonId);
		person.setFullName("Test Trader");
		person.setEmail(logonId + "@traderx.test");
		person.setDepartment("Trading");
		return person;
	}

	private void givenPeopleServiceKnows(String username) {
		when(this.restTemplate.getForEntity(contains("LogonId=" + username), eq(Person.class)))
				.thenReturn(ResponseEntity.ok(person(username)));
	}

	private void givenPeopleServiceDoesNotKnow(String username) {
		when(this.restTemplate.getForEntity(contains("LogonId=" + username), eq(Person.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
	}

	@Test
	@DisplayName("GET /accountuser/{id} returns the requested account user")
	void getAccountUserByIdReturnsAccountUser() throws Exception {
		when(this.accountUserService.getAccountUserById(1)).thenReturn(accountUser(1, "trader1"));

		this.mockMvc.perform(get("/accountuser/{id}", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").value(1))
				.andExpect(jsonPath("$.username").value("trader1"));
	}

	@Test
	@DisplayName("GET /accountuser/{id} returns 404 when the account user does not exist")
	void getAccountUserByIdReturnsNotFound() throws Exception {
		when(this.accountUserService.getAccountUserById(99))
				.thenThrow(new ResourceNotFoundException("AccountUser with id 99 not found"));

		this.mockMvc.perform(get("/accountuser/{id}", 99))
				.andExpect(status().isNotFound())
				.andExpect(content().string("AccountUser with id 99 not found"));
	}

	@Test
	@DisplayName("GET /accountuser/ returns all account users")
	void getAllAccountUsersReturnsAccountUsers() throws Exception {
		when(this.accountUserService.getAllAccountUsers())
				.thenReturn(Arrays.asList(accountUser(1, "trader1"), accountUser(2, "trader2")));

		this.mockMvc.perform(get("/accountuser/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@DisplayName("GET /accountuser/ returns an empty array when no account users exist")
	void getAllAccountUsersReturnsEmptyArray() throws Exception {
		when(this.accountUserService.getAllAccountUsers()).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/accountuser/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("POST /accountuser/ creates the account user when the person is known to People service")
	void createAccountUserSucceedsForKnownPerson() throws Exception {
		givenPeopleServiceKnows("trader1");
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenReturn(accountUser(1, "trader1"));

		this.mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(accountUser(1, "trader1"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("trader1"));

		verify(this.accountUserService).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	@DisplayName("POST /accountuser/ returns 404 when the person is unknown to People service")
	void createAccountUserReturnsNotFoundForUnknownPerson() throws Exception {
		givenPeopleServiceDoesNotKnow("ghost");

		this.mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(accountUser(1, "ghost"))))
				.andExpect(status().isNotFound())
				.andExpect(content().string("ghost not found in People service."));

		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	@DisplayName("POST /accountuser/ returns 404 when the referenced account does not exist")
	void createAccountUserReturnsNotFoundForMissingAccount() throws Exception {
		givenPeopleServiceKnows("trader1");
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class)))
				.thenThrow(new ResourceNotFoundException("Account with id 42 not found"));

		this.mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(accountUser(42, "trader1"))))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Account with id 42 not found"));
	}

	@Test
	@DisplayName("POST /accountuser/ with a malformed body never reaches the service")
	void createAccountUserRejectsMalformedBody() throws Exception {
		this.mockMvc.perform(post("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":"))
				.andExpect(status().isInternalServerError());

		verify(this.accountUserService, never()).upsertAccountUser(any(AccountUser.class));
	}

	@Test
	@DisplayName("PUT /accountuser/ updates the account user without calling People service")
	void updateAccountUserSkipsPersonValidation() throws Exception {
		when(this.accountUserService.upsertAccountUser(any(AccountUser.class))).thenReturn(accountUser(1, "trader1"));

		this.mockMvc.perform(put("/accountuser/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(accountUser(1, "trader1"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").value(1));

		verify(this.restTemplate, never()).getForEntity(any(String.class), eq(Person.class));
	}

	@Test
	@DisplayName("Unexpected service failures are mapped to 500")
	void unexpectedFailureIsMappedToInternalServerError() throws Exception {
		when(this.accountUserService.getAllAccountUsers()).thenThrow(new IllegalStateException("database unavailable"));

		this.mockMvc.perform(get("/accountuser/"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string("database unavailable"));
	}
}
