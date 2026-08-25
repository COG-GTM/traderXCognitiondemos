package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

/**
 * Edge and corner case coverage for POST /trade/.
 *
 * All outbound reference-data / account-service traffic is served by
 * {@link MockRestServiceServer}, so the tests are fully hermetic.
 */
@WebMvcTest(TradeOrderController.class)
@TestPropertySource(properties = {
		"reference.data.service.url=http://reference-data:18085",
		"account.service.url=http://account-service:18088"
})
class TradeOrderControllerTest {

	private static final String REFERENCE_DATA = "http://reference-data:18085";
	private static final String ACCOUNT_SERVICE = "http://account-service:18088";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TradeOrderController controller;

	@MockBean
	private Publisher<TradeOrder> tradePublisher;

	private MockRestServiceServer mockServer;

	/** Every outbound URI observed by the mock server, in call order. */
	private final List<String> observedUris = new ArrayList<>();

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		this.observedUris.clear();
		ReflectionTestUtils.setField(this.controller, "restTemplate", restTemplate);
	}

	// ---------------------------------------------------------------- helpers

	private void expectTickerLookup(String responseBody) {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private void expectAccountLookup(String responseBody) {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private void expectValidTickerAndAccount() {
		expectTickerLookup("{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\"}");
		expectAccountLookup("{\"id\":1,\"displayName\":\"Test Account\"}");
	}

	private MvcResult postTrade(String body) throws Exception {
		return this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andReturn();
	}

	private static Throwable rootCause(Throwable t) {
		Throwable current = t;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private static String order(String security, Integer accountId, String side, Object quantity) {
		return "{\"security\":" + json(security) + ",\"accountId\":" + accountId
				+ ",\"side\":" + json(side) + ",\"quantity\":" + quantity + "}";
	}

	private static String json(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	// ------------------------------------------------------------ happy paths

	@Test
	@DisplayName("TS-01a: a valid Buy order publishes exactly one message to /trades")
	void buyOrderPublishesOnce() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", 100));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher, times(1)).publish(eq("/trades"), captor.capture());
		assertEquals("AAPL", captor.getValue().getSecurity());
		assertEquals(100, captor.getValue().getQuantity());
		this.mockServer.verify();
		// note: the controller concatenates "//stocks/", the extra slash is collapsed by
		// RestTemplate's URI builder before the call goes out.
		assertEquals(REFERENCE_DATA + "/stocks/AAPL", this.observedUris.get(0));
		assertEquals(ACCOUNT_SERVICE + "/account/1", this.observedUris.get(1));
	}

	@Test
	@DisplayName("TS-01b: a valid Sell order publishes exactly one message to /trades")
	void sellOrderPublishesOnce() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Sell", 100));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, times(1)).publish(eq("/trades"), any(TradeOrder.class));
		this.mockServer.verify();
	}

	// --------------------------------------------------------- unknown ticker

	@Test
	@DisplayName("TS-02: unknown ticker (reference-data 404) yields 404 and publishes nothing")
	void unknownTickerYields404() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade(order("NOPE", 1, "Buy", 10));

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
		// the account service is never consulted once the ticker is rejected
		this.mockServer.verify();
		assertEquals(1, this.observedUris.size());
	}

	@Test
	@DisplayName("TS-03: unknown account (account-service 404) yields 404 and publishes nothing")
	void unknownAccountYields404() throws Exception {
		expectTickerLookup("{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\"}");
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade(order("AAPL", 987654, "Buy", 10));

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
		this.mockServer.verify();
	}

	@Test
	@DisplayName("TS-04a: reference-data 4xx that is NOT 404 is also treated as 'unknown ticker' (404)")
	void referenceDataForbiddenTreatedAsUnknownTicker() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		MvcResult result = postTrade(order("AAPL", 1, "Buy", 10));

		// The catch block swallows every HttpClientErrorException and returns false,
		// so a 403/401/429 from reference-data is reported to the caller as
		// "ticker not found" rather than as an upstream failure.
		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-04b: reference-data 500 is NOT caught; HttpServerErrorException escapes the controller")
	void referenceDataServerErrorEscapes() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withServerError());

		Exception thrown = assertThrows(Exception.class, () -> postTrade(order("AAPL", 1, "Buy", 10)));

		assertTrue(rootCause(thrown) instanceof HttpServerErrorException,
				"expected the raw HttpServerErrorException to propagate, got: " + rootCause(thrown));
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@Disabled("LATENT BUG: reference-data 5xx is not caught (only HttpClientErrorException is), "
			+ "so an upstream outage surfaces as an unhandled exception instead of 502/503")
	@DisplayName("TS-04c: reference-data 500 should map to a 5xx gateway response, not an unhandled exception")
	void referenceDataServerErrorShouldReturnGatewayError() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withServerError());

		MvcResult result = postTrade(order("AAPL", 1, "Buy", 10));

		assertEquals(HttpStatus.BAD_GATEWAY.value(), result.getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-05: reference-data unreachable -> ResourceAccessException escapes the controller")
	void referenceDataUnreachableEscapes() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(request -> {
					throw new ResourceAccessException("Connection refused");
				});

		Exception thrown = assertThrows(Exception.class, () -> postTrade(order("AAPL", 1, "Buy", 10)));

		assertTrue(rootCause(thrown) instanceof ResourceAccessException,
				"expected ResourceAccessException, got: " + rootCause(thrown));
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-05b: account-service unreachable -> ResourceAccessException escapes the controller")
	void accountServiceUnreachableEscapes() throws Exception {
		expectTickerLookup("{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\"}");
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(request -> {
					throw new ResourceAccessException("Connection refused");
				});

		Exception thrown = assertThrows(Exception.class, () -> postTrade(order("AAPL", 1, "Buy", 10)));

		assertTrue(rootCause(thrown) instanceof ResourceAccessException,
				"expected ResourceAccessException, got: " + rootCause(thrown));
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	// ------------------------------------------------------ quantity boundaries

	@Test
	@DisplayName("TS-06a: quantity 0 is accepted and published (no validation)")
	void zeroQuantityIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", 0));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(0, captor.getValue().getQuantity());
	}

	@Test
	@Disabled("LATENT BUG: TradeOrderController performs no quantity validation, so a zero-quantity "
			+ "order is published to /trades and booked downstream")
	@DisplayName("TS-06b: quantity 0 should be rejected with 400")
	void zeroQuantityShouldBeRejected() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", 0));

		assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-06c: negative quantity is accepted and published (no validation)")
	void negativeQuantityIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", -500));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(-500, captor.getValue().getQuantity());
	}

	@Test
	@Disabled("LATENT BUG: a negative quantity Buy is accepted and inverts the position sign "
			+ "downstream (trade-processor multiplies by +1 for Buy)")
	@DisplayName("TS-06d: negative quantity should be rejected with 400")
	void negativeQuantityShouldBeRejected() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", -500));

		assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-06e: quantity 1 (minimum sensible order) is accepted")
	void quantityOneIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		assertEquals(HttpStatus.OK.value(), postTrade(order("AAPL", 1, "Buy", 1)).getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-06f: quantity Integer.MAX_VALUE is accepted and published verbatim")
	void maxIntQuantityIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", Integer.MAX_VALUE));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(Integer.MAX_VALUE, captor.getValue().getQuantity());
	}

	@Test
	@DisplayName("TS-06g: quantity 2147483648 (int overflow) is rejected with 400 by Jackson")
	void overflowQuantityIsRejected() throws Exception {
		MvcResult result = postTrade(order("AAPL", 1, "Buy", "2147483648"));

		assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
		this.mockServer.verify(); // no outbound calls at all
	}

	// -------------------------------------------------------------- null fields

	@Test
	@DisplayName("TS-07a: null quantity is accepted and published as null")
	void nullQuantityIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade(order("AAPL", 1, "Buy", null));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(null, captor.getValue().getQuantity());
	}

	@Test
	@DisplayName("TS-07b: null security produces an outbound lookup for the literal string 'null'")
	void nullSecurityIsConcatenatedIntoTheUrl() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade("{\"security\":null,\"accountId\":1,\"side\":\"Buy\",\"quantity\":10}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(REFERENCE_DATA + "/stocks/null", this.observedUris.get(0));
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-07c: null accountId produces an outbound lookup for the literal string 'null'")
	void nullAccountIdIsConcatenatedIntoTheUrl() throws Exception {
		expectTickerLookup("{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\"}");
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade("{\"security\":\"AAPL\",\"accountId\":null,\"side\":\"Buy\",\"quantity\":10}");

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(ACCOUNT_SERVICE + "/account/null", this.observedUris.get(1));
	}

	@Test
	@DisplayName("TS-07d: null side is accepted and published (no validation of trade direction)")
	void nullSideIsAccepted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = postTrade("{\"security\":\"AAPL\",\"accountId\":1,\"side\":null,\"quantity\":10}");

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(null, captor.getValue().getSide());
	}

	@Test
	@Disabled("LATENT BUG: a TradeOrder with a null side is published; trade-processor calls "
			+ "side.equals(...) style logic on it and cannot tell Buy from Sell")
	@DisplayName("TS-07e: null side should be rejected with 400")
	void nullSideShouldBeRejected() throws Exception {
		expectValidTickerAndAccount();

		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade("{\"security\":\"AAPL\",\"accountId\":1,\"side\":null,\"quantity\":10}")
						.getResponse().getStatus());
	}

	// ------------------------------------------------------- security oddities

	@Test
	@DisplayName("TS-08a: empty-string security is looked up against the bare /stocks/ path")
	void emptySecurityIsLookedUp() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade(order("", 1, "Buy", 10));

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(REFERENCE_DATA + "/stocks/", this.observedUris.get(0));
	}

	@Test
	@DisplayName("TS-08b: whitespace-only security degrades into a call to the /stocks collection endpoint")
	void whitespaceSecurityHitsTheCollectionEndpoint() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withSuccess("[{\"ticker\":\"AAPL\"}]", MediaType.APPLICATION_JSON));

		Exception thrown = assertThrows(Exception.class, () -> postTrade(order("   ", 1, "Buy", 10)));

		// The blank ticker is trimmed away while building the URI, so the request lands on
		// the reference-data *list* endpoint. The array body cannot be read as a Security,
		// and the resulting RestClientException is not handled anywhere.
		assertEquals(REFERENCE_DATA + "/stocks", this.observedUris.get(0));
		assertTrue(rootCause(thrown) instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException,
				"unexpected root cause: " + rootCause(thrown));
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@Disabled("LATENT BUG: a whitespace-only ticker collapses to a GET of the /stocks collection endpoint "
			+ "instead of being rejected, and the unreadable array response escapes as an unhandled exception")
	@DisplayName("TS-08b2: whitespace-only security should be rejected with 400")
	void whitespaceSecurityShouldBeRejected() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withSuccess("[{\"ticker\":\"AAPL\"}]", MediaType.APPLICATION_JSON));

		MvcResult result = postTrade(order("   ", 1, "Buy", 10));

		assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-08c: a ticker containing path separators is concatenated straight into the outbound URL")
	void tickerWithPathTraversalIsNotSanitised() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade(order("../../account/1", 1, "Buy", 10));

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		String uri = this.observedUris.get(0);
		// The ticker is never validated or encoded as a single path segment, so the
		// caller controls the outbound path: a path-traversal / SSRF surface.
		assertTrue(uri.contains("../../account/1"), "unexpected URI: " + uri);
	}

	@Test
	@Disabled("LATENT BUG: TradeOrderController.validateTicker builds the reference-data URL by raw "
			+ "string concatenation, so a ticker such as '../../account/1' escapes the /stocks/ path")
	@DisplayName("TS-08d: a ticker containing path separators should not be able to escape /stocks/")
	void tickerWithPathTraversalShouldBeEncoded() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		postTrade(order("../../account/1", 1, "Buy", 10));

		String uri = this.observedUris.get(0);
		assertTrue(uri.startsWith(REFERENCE_DATA + "/stocks/") && !uri.contains(".."),
				"ticker escaped its path segment: " + uri);
	}

	@Test
	@DisplayName("TS-09a: a security longer than the 50 char DB column is accepted and published")
	void overlongSecurityIsAccepted() throws Exception {
		String longTicker = "A".repeat(120);
		expectTickerLookup("{\"ticker\":\"" + longTicker + "\",\"companyName\":\"Long\"}");
		expectAccountLookup("{\"id\":1,\"displayName\":\"Test Account\"}");

		MvcResult result = postTrade(order(longTicker, 1, "Buy", 10));

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(120, captor.getValue().getSecurity().length());
	}

	@Test
	@Disabled("LATENT BUG: security is not length-checked against the 50 char SECURITY column, so an "
			+ "overlong ticker is only rejected later, when trade-processor tries to persist the trade")
	@DisplayName("TS-09b: a security longer than 50 chars should be rejected up-front")
	void overlongSecurityShouldBeRejected() throws Exception {
		String longTicker = "A".repeat(120);
		expectTickerLookup("{\"ticker\":\"x\",\"companyName\":\"Long\"}");
		expectAccountLookup("{\"id\":1,\"displayName\":\"Test Account\"}");

		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade(order(longTicker, 1, "Buy", 10)).getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-09c: a unicode ticker is percent-encoded in the outbound reference-data call")
	void unicodeTickerIsEncoded() throws Exception {
		this.mockServer.expect(request -> this.observedUris.add(request.getURI().toString()))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		MvcResult result = postTrade(order("\u00c9\u00c9\u00c9", 1, "Buy", 10));

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		assertEquals(REFERENCE_DATA + "/stocks/%C3%89%C3%89%C3%89", this.observedUris.get(0));
	}

	// ------------------------------------------------------------- bad payloads

	@Test
	@DisplayName("TS-10a: side 'BUY' (wrong case) is rejected with 400 by enum deserialisation")
	void upperCaseSideIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade(order("AAPL", 1, "BUY", 10)).getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-10b: side 'buy' (lower case) is rejected with 400")
	void lowerCaseSideIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade(order("AAPL", 1, "buy", 10)).getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-10c: an unknown side 'Short' is rejected with 400")
	void unknownSideIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade(order("AAPL", 1, "Short", 10)).getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-11a: malformed JSON is rejected with 400 and never reaches reference-data")
	void malformedJsonIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				postTrade("{\"security\":\"AAPL\",").getResponse().getStatus());
		this.mockServer.verify();
		verify(this.tradePublisher, never()).publish(any(), any());
	}

	@Test
	@DisplayName("TS-11b: an empty body is rejected with 400")
	void emptyBodyIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(), postTrade("").getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-11c: a JSON body of 'null' is rejected with 400")
	void nullBodyIsRejected() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(), postTrade("null").getResponse().getStatus());
	}

	// -------------------------------------------------------- trust boundaries

	@Test
	@DisplayName("TS-12a: a client-supplied id is trusted and echoed back verbatim")
	void clientSuppliedIdIsTrusted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"client-chosen-id\",\"security\":\"AAPL\",\"accountId\":1,"
						+ "\"side\":\"Buy\",\"quantity\":10}"))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		// trade-processor ignores this id and generates its own UUID, but the id is
		// still accepted here and echoed to the caller.
		assertEquals("client-chosen-id", captor.getValue().getId());
		assertTrue(result.getResponse().getContentAsString().contains("client-chosen-id"));
	}

	@Test
	@DisplayName("TS-12b: a client-supplied state is trusted and forwarded onto the trade feed")
	void clientSuppliedStateIsTrusted() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"Settled\",\"security\":\"AAPL\",\"accountId\":1,"
						+ "\"side\":\"Buy\",\"quantity\":10}"))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		// TradeOrder.state has no setter, yet Jackson still binds it: the caller can
		// pre-set the lifecycle state of the order it submits.
		assertEquals("Settled", captor.getValue().getState());
	}

	@Test
	@Disabled("LATENT BUG: TradeOrder.state is bound from the request body, so a client can submit an "
			+ "order that already claims to be Settled; the field should be server-owned")
	@DisplayName("TS-12b2: a client-supplied state should be ignored by the server")
	void clientSuppliedStateShouldBeIgnored() throws Exception {
		expectValidTickerAndAccount();

		this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"Settled\",\"security\":\"AAPL\",\"accountId\":1,"
						+ "\"side\":\"Buy\",\"quantity\":10}"))
				.andReturn();

		ArgumentCaptor<TradeOrder> captor = ArgumentCaptor.forClass(TradeOrder.class);
		verify(this.tradePublisher).publish(eq("/trades"), captor.capture());
		assertEquals(null, captor.getValue().getState());
	}

	@Test
	@DisplayName("TS-12c: unknown JSON properties are ignored rather than rejected")
	void unknownPropertiesAreIgnored() throws Exception {
		expectValidTickerAndAccount();

		MvcResult result = this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"security\":\"AAPL\",\"accountId\":1,\"side\":\"Buy\","
						+ "\"quantity\":10,\"price\":0.01,\"bogus\":true}"))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
	}

	// ------------------------------------------------------------- publisher

	@Test
	@DisplayName("TS-13: a PubSubException is wrapped in RuntimeException and escapes as a 500")
	void pubSubExceptionIsWrapped() throws Exception {
		expectValidTickerAndAccount();
		doThrow(new PubSubException("feed down")).when(this.tradePublisher).publish(eq("/trades"), any());

		Exception thrown = assertThrows(Exception.class, () -> postTrade(order("AAPL", 1, "Buy", 10)));

		Throwable root = rootCause(thrown);
		assertTrue(root instanceof PubSubException, "expected PubSubException at the root, got: " + root);
		assertNotNull(root.getMessage());
	}

	// ---------------------------------------------------------- protocol edges

	@Test
	@DisplayName("TS-14a: a text/plain body is rejected with 415")
	void textPlainBodyIsRejected() throws Exception {
		MvcResult result = this.mockMvc.perform(post("/trade/")
				.contentType(MediaType.TEXT_PLAIN)
				.content(order("AAPL", 1, "Buy", 10)))
				.andReturn();

		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), result.getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-14b: a missing Content-Type is rejected with 415")
	void missingContentTypeIsRejected() throws Exception {
		MvcResult result = this.mockMvc.perform(post("/trade/").content(order("AAPL", 1, "Buy", 10)))
				.andReturn();

		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), result.getResponse().getStatus());
	}

	@Test
	@DisplayName("TS-15: POST /trade (no trailing slash) does not match the '/' mapping")
	void postWithoutTrailingSlashIsNotMapped() throws Exception {
		MvcResult result = this.mockMvc.perform(post("/trade")
				.contentType(MediaType.APPLICATION_JSON)
				.content(order("AAPL", 1, "Buy", 10)))
				.andReturn();

		assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
		verify(this.tradePublisher, never()).publish(any(), any());
	}
}
