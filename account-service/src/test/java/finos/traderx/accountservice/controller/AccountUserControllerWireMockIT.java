package finos.traderx.accountservice.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.repository.AccountRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Layer B — out-of-process integration tests. A full {@code account-service} boots on a random
 * port with an in-memory H2 database (no external DB, no .NET runtime), and its dependency on
 * {@code people-service} is redirected — purely via the {@code people.service.url} config seam —
 * to a JVM WireMock server. WireMock is used because it can simulate transport-level failures
 * (5xx, connection reset, delay, malformed body) that a spec stub cannot.
 *
 * Covers S1 (success), S4 (500 dependency error), S5 (connection error), S6 (timeout/slow) and
 * S7 (malformed body). Each ⚠️ scenario documents how account-service currently degrades.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountUserControllerWireMockIT {

	private static final WireMockServer WIREMOCK = new WireMockServer(options().dynamicPort());

	static {
		WIREMOCK.start();
	}

	@AfterAll
	static void stopWireMock() {
		WIREMOCK.stop();
	}

	@DynamicPropertySource
	static void peopleServiceProperties(DynamicPropertyRegistry registry) {
		registry.add("people.service.url", WIREMOCK::baseUrl);
		registry.add("people.service.read-timeout-ms", () -> "1000");
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AccountRepository accountRepository;

	@BeforeEach
	void resetWireMock() {
		WIREMOCK.resetAll();
	}

	private ResponseEntity<String> postAccountUser(int accountId, String username) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = "{\"accountId\":" + accountId + ",\"username\":\"" + username + "\"}";
		return restTemplate.postForEntity("/accountuser/", new HttpEntity<>(body, headers), String.class);
	}

	@Test
	void s1_personFound_createsAccountUser() {
		Account account = new Account();
		account.setDisplayName("Test Account");
		int accountId = accountRepository.save(account).getId();

		WIREMOCK.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo("jdoe"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"logonId\":\"jdoe\",\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\"}")));

		ResponseEntity<String> response = postAccountUser(accountId, "jdoe");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().contains("jdoe"));
	}

	@Test
	void s4_dependencyReturns500_degradesTo500() {
		WIREMOCK.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.willReturn(aResponse().withStatus(500)));

		// FINDING: a 5xx from people-service is not caught (only HttpClientErrorException is),
		// so it bubbles to the generic handler and account-service returns 500 rather than a
		// dependency-specific 502/503.
		ResponseEntity<String> response = postAccountUser(1, "jdoe");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	void s5_connectionError_degradesTo500() {
		WIREMOCK.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		// FINDING: a transport failure raises ResourceAccessException (uncaught) -> 500, with no
		// indication that the dependency was simply unreachable.
		ResponseEntity<String> response = postAccountUser(1, "jdoe");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	void s6_slowDependency_failsFastWithinTimeoutBudget() {
		WIREMOCK.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.willReturn(aResponse()
						.withFixedDelay(3000)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"logonId\":\"jdoe\"}")));

		long startMs = System.currentTimeMillis();
		ResponseEntity<String> response = postAccountUser(1, "jdoe");
		long elapsedMs = System.currentTimeMillis() - startMs;

		// With the injectable RestTemplate configured with a 1s read timeout, the call fails fast
		// instead of hanging on the 3s delay. FINDING: it still surfaces as a generic 500.
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(elapsedMs < 2500, "Expected fail-fast within read-timeout budget, took " + elapsedMs + "ms");
	}

	@Test
	void s7_malformedBody_degradesTo500() {
		WIREMOCK.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("<<not json>>")));

		// FINDING: a malformed 200 body raises RestClientException (uncaught) -> 500.
		ResponseEntity<String> response = postAccountUser(1, "jdoe");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}
}
