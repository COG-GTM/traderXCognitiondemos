package finos.traderx.accountservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountUserRepository;
import finos.traderx.accountservice.service.AccountService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "/test-application.properties")
class AccountUserIntegrationTests {

	private static final WireMockServer wireMockServer =
			new WireMockServer(WireMockConfiguration.options().dynamicPort());

	static {
		wireMockServer.start();
	}

	@DynamicPropertySource
	static void peopleServiceProperties(DynamicPropertyRegistry registry) {
		registry.add("people.service.url", () -> "http://localhost:" + wireMockServer.port());
	}

	@AfterAll
	static void stopWireMock() {
		wireMockServer.stop();
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountUserRepository accountUserRepository;

	@BeforeEach
	void resetWireMock() {
		wireMockServer.resetAll();
	}

	private int seedAccount(String displayName) {
		Account account = new Account();
		account.setDisplayName(displayName);
		return accountService.upsertAccount(account).getId();
	}

	private void stubPersonFound(String logonId) {
		String body = "{"
				+ "\"logonId\":\"" + logonId + "\","
				+ "\"fullName\":\"Test User\","
				+ "\"email\":\"" + logonId + "@example.com\","
				+ "\"department\":\"Engineering\","
				+ "\"photoUrl\":\"http://example.com/photo.png\""
				+ "}";
		wireMockServer.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo(logonId))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(body)));
	}

	private void stubPersonNotFound(String logonId) {
		wireMockServer.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo(logonId))
				.willReturn(aResponse().withStatus(404)));
	}

	private AccountUser buildAccountUser(int accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

	private boolean isPersisted(int accountId, String username) {
		for (AccountUser user : accountUserRepository.findAll()) {
			if (user.getAccountId() == accountId && username.equals(user.getUsername())) {
				return true;
			}
		}
		return false;
	}

	@Test
	void createsAccountUserWhenPersonExists() {
		int accountId = seedAccount("happy path account");
		String username = "alice";
		stubPersonFound(username);

		ResponseEntity<AccountUser> response = restTemplate.postForEntity(
				"/accountuser/", buildAccountUser(accountId, username), AccountUser.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(isPersisted(accountId, username), "AccountUser should be persisted");
	}

	@Test
	void returnsNotFoundWhenPersonMissing() {
		int accountId = seedAccount("missing person account");
		String username = "ghost";
		stubPersonNotFound(username);

		ResponseEntity<String> response = restTemplate.postForEntity(
				"/accountuser/", buildAccountUser(accountId, username), String.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertFalse(isPersisted(accountId, username), "AccountUser should not be persisted");
	}

	@Test
	void returnsNotFoundWhenAccountMissing() {
		int missingAccountId = 999999;
		String username = "bob";
		stubPersonFound(username);

		ResponseEntity<String> response = restTemplate.postForEntity(
				"/accountuser/", buildAccountUser(missingAccountId, username), String.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertFalse(isPersisted(missingAccountId, username), "AccountUser should not be persisted");
	}
}
