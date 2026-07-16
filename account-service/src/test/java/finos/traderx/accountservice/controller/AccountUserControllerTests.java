package finos.traderx.accountservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountService;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "/test-application.properties")
class AccountUserControllerTests {

	private static final MockWebServer mockWebServer = new MockWebServer();

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AccountService accountService;

	@AfterAll
	static void stopMockServer() throws IOException {
		mockWebServer.shutdown();
	}

	@DynamicPropertySource
	static void peopleServiceProperties(DynamicPropertyRegistry registry) throws IOException {
		mockWebServer.start();
		String url = mockWebServer.url("/").toString();
		String baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		registry.add("people.service.url", () -> baseUrl);
	}

	@Test
	void createAccountUserWhenPersonExists() throws Exception {
		String personJson = "{"
				+ "\"logonId\":\"jdoe\","
				+ "\"fullName\":\"John Doe\","
				+ "\"email\":\"jdoe@example.com\","
				+ "\"department\":\"Trading\","
				+ "\"photoUrl\":\"http://example.com/jdoe.png\""
				+ "}";
		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody(personJson));

		Account account = new Account();
		account.setDisplayName("test account");
		Integer accountId = accountService.upsertAccount(account).getId();

		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername("jdoe");

		ResponseEntity<AccountUser> response = restTemplate.postForEntity(
				"/accountuser/", accountUser, AccountUser.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getAccountId()).isEqualTo(accountId);
		assertThat(response.getBody().getUsername()).isEqualTo("jdoe");

		RecordedRequest recordedRequest = mockWebServer.takeRequest();
		assertThat(recordedRequest.getPath()).isEqualTo("/People/GetPerson?LogonId=jdoe");
	}

	@Test
	void createAccountUserWhenPersonNotFound() throws Exception {
		mockWebServer.enqueue(new MockResponse().setResponseCode(404));

		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(2);
		accountUser.setUsername("missing");

		ResponseEntity<String> response = restTemplate.postForEntity(
				"/accountuser/", accountUser, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		RecordedRequest recordedRequest = mockWebServer.takeRequest();
		assertThat(recordedRequest.getPath()).isEqualTo("/People/GetPerson?LogonId=missing");
	}
}
