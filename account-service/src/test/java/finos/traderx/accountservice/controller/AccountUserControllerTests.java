package finos.traderx.accountservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.model.Person;
import finos.traderx.accountservice.security.AccountAdminAuthorizer;
import finos.traderx.accountservice.service.AccountUserService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(AccountUserController.class)
@Import(AccountAdminAuthorizer.class)
@TestPropertySource(properties = {
		"people.service.url=http://people-service:18089",
		"traderx.account-admin.token=test-admin-token-0123456789"
})
class AccountUserControllerTests {

	private static final String ADMIN = "Bearer test-admin-token-0123456789";

	@Autowired
	MockMvc mockMvc;

	@MockBean
	AccountUserService accountUserService;

	@MockBean
	RestTemplate restTemplate;

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static Person person(String logonId) {
		Person person = new Person();
		person.setLogonId(logonId);
		return person;
	}

	private static AccountUser accountUser(int accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

	@Test
	void putWithoutTokenIsRejectedBeforeAnyLookupOrWrite() throws Exception {
		this.mockMvc.perform(json(put("/accountuser/"), "{\"accountId\":22214,\"username\":\"attacker\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
		verifyNoInteractions(this.accountUserService, this.restTemplate);
	}

	@Test
	void postWithoutTokenIsRejected() throws Exception {
		this.mockMvc.perform(json(post("/accountuser/"), "{\"accountId\":22214,\"username\":\"user01\"}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(this.accountUserService, this.restTemplate);
	}

	@Test
	void wrongTokenIsForbidden() throws Exception {
		this.mockMvc.perform(json(put("/accountuser/"), "{\"accountId\":22214,\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-the-token"))
				.andExpect(status().isForbidden());
		verifyNoInteractions(this.accountUserService, this.restTemplate);
	}

	@Test
	void putValidatesPersonBeforeUpserting() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class)))
				.thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

		this.mockMvc.perform(json(put("/accountuser/"), "{\"accountId\":22214,\"username\":\"ghost\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isNotFound());
		verify(this.accountUserService, never()).upsertAccountUser(any(), any());
	}

	@Test
	void putWithAdminTokenAndKnownPersonUpserts() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class))).thenReturn(person("user01"));
		when(this.accountUserService.upsertAccountUser(22214, "user01")).thenReturn(accountUser(22214, "user01"));

		this.mockMvc.perform(json(put("/accountuser/"), "{\"accountId\":22214,\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").value(22214))
				.andExpect(jsonPath("$.username").value("user01"));
	}

	@Test
	void usernameWithUrlMetacharactersIsRejectedWithoutCallingPeopleService() throws Exception {
		for (String username : new String[] { "user01#x", "user01&LogonId=user02", "user01?a", "user 01", "", "abcdefghijklmnop" }) {
			String body = "{\"accountId\":22214,\"username\":\"" + username + "\"}";
			this.mockMvc.perform(json(post("/accountuser/"), body).header(HttpHeaders.AUTHORIZATION, ADMIN))
					.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(this.accountUserService, this.restTemplate);
	}

	@Test
	void peopleServiceUrlIsBuiltWithEncodedQueryParam() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class))).thenReturn(person("j.smith-01"));
		when(this.accountUserService.createAccountUser(22214, "j.smith-01")).thenReturn(accountUser(22214, "j.smith-01"));

		this.mockMvc.perform(json(post("/accountuser/"), "{\"accountId\":22214,\"username\":\"j.smith-01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isOk());

		ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
		verify(this.restTemplate).getForObject(uri.capture(), eq(Person.class));
		assertEquals("http://people-service:18089/People/GetPerson?LogonId=j.smith-01", uri.getValue().toString());
		assertEquals(null, uri.getValue().getFragment());
	}

	@Test
	void personWhoseLogonIdDiffersFromRequestIsRejected() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class))).thenReturn(person("user02"));

		this.mockMvc.perform(json(post("/accountuser/"), "{\"accountId\":22214,\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isNotFound());
		verifyNoInteractions(this.accountUserService);
	}

	@Test
	void postStoresOnlyAccountIdAndPeopleServiceLogonId() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class))).thenReturn(person("user01"));
		when(this.accountUserService.createAccountUser(22214, "user01")).thenReturn(accountUser(22214, "user01"));

		this.mockMvc.perform(json(post("/accountuser/"), "{\"id\":99,\"accountId\":22214,\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isOk());
		verify(this.accountUserService).createAccountUser(22214, "user01");
	}

	@Test
	void postOfExistingMappingConflicts() throws Exception {
		when(this.restTemplate.getForObject(any(URI.class), eq(Person.class))).thenReturn(person("user01"));
		when(this.accountUserService.createAccountUser(22214, "user01"))
				.thenThrow(new AccountUserConflictException("user01 is already mapped to account 22214"));

		this.mockMvc.perform(json(post("/accountuser/"), "{\"accountId\":22214,\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isConflict());
	}

	@Test
	void missingAccountIdIsBadRequest() throws Exception {
		this.mockMvc.perform(json(put("/accountuser/"), "{\"username\":\"user01\"}")
				.header(HttpHeaders.AUTHORIZATION, ADMIN))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(this.accountUserService, this.restTemplate);
	}
}
