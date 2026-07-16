package finos.traderx.accountservice.contract;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.atlassian.oai.validator.wiremock.OpenApiValidationListener;
import com.github.tomakehurst.wiremock.WireMockServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * §4 Contract conformance. Attaches Atlassian's {@link OpenApiValidationListener}, loaded from the
 * checked-in {@code people-service/openapi.yaml}, to a WireMock server. Every response the mock
 * serves is validated against the real contract, so a hand-written WireMock stub cannot silently
 * drift from the spec: a spec-faithful stub passes validation, a violating stub fails it.
 */
class PeopleServiceContractTest {

	private static WireMockServer wireMock;
	private static OpenApiValidationListener validationListener;

	@BeforeAll
	static void startWireMock() throws IOException {
		String spec = loadSpec();
		validationListener = new OpenApiValidationListener(spec);
		wireMock = new WireMockServer(options().dynamicPort());
		wireMock.addMockServiceRequestListener(validationListener);
		wireMock.start();
	}

	@AfterEach
	void resetValidation() {
		validationListener.reset();
		wireMock.resetAll();
	}

	@AfterAll
	static void stopWireMock() {
		wireMock.stop();
	}

	private static String loadSpec() throws IOException {
		try (InputStream in = PeopleServiceContractTest.class
				.getResourceAsStream("/people-service-openapi.yaml")) {
			if (in == null) {
				throw new IllegalStateException("people-service-openapi.yaml not found on the test classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void getPerson(String logonId) {
		// We only care that WireMock serves the response so the listener validates it against the
		// contract; the client's own handling of error statuses is irrelevant here.
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
				return false;
			}
		});
		restTemplate.getForEntity(
				wireMock.baseUrl() + "/People/GetPerson?LogonId=" + logonId, String.class);
	}

	@Test
	void specFaithfulStub_passesContractValidation() {
		wireMock.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo("jdoe"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"logonId\":\"jdoe\",\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\","
								+ "\"employeeId\":\"E1\",\"department\":\"Trading\",\"photoUrl\":null}")));

		getPerson("jdoe");

		validationListener.assertValidationPassed();
	}

	@Test
	void notFoundStub_matchesSpecStatus_passesContractValidation() {
		wireMock.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo("ghost"))
				.willReturn(aResponse().withStatus(404)));

		getPerson("ghost");

		validationListener.assertValidationPassed();
	}

	@Test
	void driftingStub_failsContractValidation() {
		// A stub whose Person shape violates the contract (logonId typed as a number, which the
		// spec declares as a string). The listener must reject it, proving drift is caught.
		wireMock.stubFor(get(urlPathEqualTo("/People/GetPerson"))
				.withQueryParam("LogonId", equalTo("jdoe"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"logonId\":12345}")));

		getPerson("jdoe");

		assertThrows(OpenApiValidationListener.OpenApiValidationException.class,
				() -> validationListener.assertValidationPassed());
	}
}
