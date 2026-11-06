package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class TradeOrderControllerTickerValidationTest {

	private static final String REF_DATA = "http://reference-data:18085";
	private static final String ACCOUNTS = "http://account-service:18088";

	private TradeOrderController controller;
	private Publisher<TradeOrder> publisher;
	private MockRestServiceServer server;
	private final List<String> requestedUris = new ArrayList<>();

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		controller = new TradeOrderController();
		publisher = mock(Publisher.class);
		RestTemplate restTemplate = new RestTemplate();
		ReflectionTestUtils.setField(controller, "tradePublisher", publisher);
		ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", REF_DATA);
		ReflectionTestUtils.setField(controller, "accountServiceAddress", ACCOUNTS);
		server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
	}

	private void stubReferenceDataAlwaysReturns(String ticker) {
		server.expect(ExpectedCount.manyTimes(), request -> {
			String uri = request.getURI().toString();
			requestedUris.add(uri);
			if (!uri.startsWith(REF_DATA)) {
				throw new AssertionError("unexpected request " + uri);
			}
		}).andRespond(withSuccess("{\"ticker\":\"" + ticker + "\",\"companyName\":\"Microsoft\"}", MediaType.APPLICATION_JSON));
	}

	private TradeOrder order(String security) {
		return new TradeOrder("TRADE-1", 22214, security, TradeSide.Buy, 100);
	}

	@ParameterizedTest
	@ValueSource(strings = { "MSFT?x=1", "MSFT?x=FAKECO", "../health", "MSFT#a", "MSFT/../health", "..", "MSFT%3Fx", "msft", "" })
	void rejectsTickersThatWouldAlterTheReferenceDataUrl(String ticker) throws Exception {
		stubReferenceDataAlwaysReturns("MSFT");

		assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(ticker)));

		assertEquals(List.of(), requestedUris);
		verify(publisher, never()).publish(anyString(), any());
	}

	@Test
	void rejectsNullTicker() throws Exception {
		assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(null)));
		verify(publisher, never()).publish(anyString(), any());
	}

	@Test
	void rejectsWhenReferenceDataReturnsADifferentTicker() throws Exception {
		stubReferenceDataAlwaysReturns("AAPL");

		assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order("MSFT")));
		verify(publisher, never()).publish(anyString(), any());
	}

	@Test
	void acceptsValidTickerUsingASinglePathSegment() throws Exception {
		server.expect(requestTo(REF_DATA + "/stocks/BRK.B"))
				.andRespond(withSuccess("{\"ticker\":\"BRK.B\",\"companyName\":\"Berkshire Hathaway\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(ACCOUNTS + "/account/22214"))
				.andRespond(withSuccess("{\"id\":22214,\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));

		TradeOrder order = order("BRK.B");
		assertEquals(order, controller.createTradeOrder(order).getBody());

		server.verify();
		verify(publisher).publish("/trades", order);
	}
}
