package finos.traderx.tradeservice.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

/**
 * Integration test that drives a real {@link TradeOrder} through
 * {@code TradeOrderController.createTradeOrder} and the controller's inline
 * {@code RestTemplate} calls to the reference-data / account services, which are
 * stubbed by an in-process WireMock HTTP server. The trade-feed publisher is
 * mocked so no real Socket.IO connection is attempted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TradeOrderControllerIntegrationTest {

	private static final WireMockServer wireMockServer =
			new WireMockServer(options().dynamicPort());

	static {
		wireMockServer.start();
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private Publisher<TradeOrder> tradePublisher;

	@DynamicPropertySource
	static void overrideServiceUrls(DynamicPropertyRegistry registry) {
		// The controller builds URLs as base + "//stocks/..." / base + "//account/...",
		// so point both at the WireMock base URL (no trailing slash).
		registry.add("reference.data.service.url", wireMockServer::baseUrl);
		registry.add("account.service.url", wireMockServer::baseUrl);
		// Avoid any attempt to open a real Socket.IO connection to the trade-feed.
		registry.add("trade.feed.address", () -> "http://localhost:1");
	}

	@AfterAll
	static void stopWireMock() {
		wireMockServer.stop();
	}

	@BeforeEach
	void resetStubs() {
		wireMockServer.resetAll();
	}

	private TradeOrder sampleOrder(String ticker, int accountId) {
		return new TradeOrder("trade-1", accountId, ticker, TradeSide.Buy, 100);
	}

	@Test
	void validTickerAndAccountPublishesTrade() throws Exception {
		String ticker = "AAPL";
		int accountId = 1234;

		// The controller builds the URL as base + "//stocks/" + ticker; RestTemplate
		// normalizes the double slash, so match one-or-more leading slashes.
		wireMockServer.stubFor(get(urlPathMatching("/+stocks/" + ticker))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"ticker\":\"" + ticker + "\",\"companyName\":\"Apple Inc.\"}")));

		wireMockServer.stubFor(get(urlPathMatching("/+account/" + accountId))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":" + accountId + ",\"displayName\":\"Test Account\"}")));

		ResponseEntity<TradeOrder> response =
				restTemplate.postForEntity("/trade/", sampleOrder(ticker, accountId), TradeOrder.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));
	}

	@Test
	void unknownTickerReturnsNotFoundAndDoesNotPublish() throws Exception {
		String ticker = "NOPE";
		int accountId = 1234;

		wireMockServer.stubFor(get(urlPathMatching("/+stocks/" + ticker))
				.willReturn(aResponse().withStatus(404)));

		ResponseEntity<String> response =
				restTemplate.postForEntity("/trade/", sampleOrder(ticker, accountId), String.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		verify(tradePublisher, never()).publish(anyString(), any(TradeOrder.class));
	}
}
