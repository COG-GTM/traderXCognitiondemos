package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class TradeOrderControllerTest {

	private static final String REFERENCE_DATA = "http://reference-data:18085";
	private static final String ACCOUNT_SERVICE = "http://account-service:18088";

	private TradeOrderController controller;
	private Publisher<TradeOrder> publisher;
	private MockRestServiceServer server;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		controller = new TradeOrderController();
		publisher = mock(Publisher.class);
		ReflectionTestUtils.setField(controller, "tradePublisher", publisher);
		ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", REFERENCE_DATA);
		ReflectionTestUtils.setField(controller, "accountServiceAddress", ACCOUNT_SERVICE);
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void publishesValidTrade() throws Exception {
		expectStock("AAPL", "{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\"}");
		expectAccount(22214);

		TradeOrder order = order("AAPL");
		assertEquals(order, controller.createTradeOrder(order).getBody());

		server.verify();
		verify(publisher).publish("/trades", order);
	}

	@Test
	void acceptsDottedShareClassTicker() throws Exception {
		expectStock("BRK.B", "{\"ticker\":\"BRK.B\",\"companyName\":\"Berkshire Hathaway\"}");
		expectAccount(22214);

		controller.createTradeOrder(order("BRK.B"));

		server.verify();
		verify(publisher).publish(anyString(), any(TradeOrder.class));
	}

	@ParameterizedTest
	@ValueSource(strings = { "AAPL?x=1", "AAPL#x", "AAPL/../MSFT", "AAPL%3Fx", "aapl", "AAPL ", "", "TOOLONGTICKER" })
	void rejectsMalformedTickerWithoutCallingReferenceData(String ticker) throws Exception {
		assertThrows(InvalidTradeOrderException.class, () -> controller.createTradeOrder(order(ticker)));

		server.verify();
		verify(publisher, never()).publish(anyString(), any(TradeOrder.class));
	}

	@Test
	void rejectsNullTicker() throws Exception {
		assertThrows(InvalidTradeOrderException.class, () -> controller.createTradeOrder(order(null)));
		verify(publisher, never()).publish(anyString(), any(TradeOrder.class));
	}

	@Test
	void rejectsWhenReferenceDataReturnsDifferentTicker() throws Exception {
		expectStock("BRK.B", "{\"ticker\":\"BRK\",\"companyName\":\"Berkshire\"}");

		assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order("BRK.B")));

		server.verify();
		verify(publisher, never()).publish(anyString(), any(TradeOrder.class));
	}

	@Test
	void rejectsUnknownTicker() throws Exception {
		server.expect(requestTo(REFERENCE_DATA + "/stocks/ZZZZ"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order("ZZZZ")));

		server.verify();
		verify(publisher, never()).publish(anyString(), any(TradeOrder.class));
	}

	private void expectStock(String ticker, String body) {
		server.expect(requestTo(REFERENCE_DATA + "/stocks/" + ticker))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	private void expectAccount(int id) {
		server.expect(requestTo(ACCOUNT_SERVICE + "/account/" + id))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("{\"id\":" + id + ",\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));
	}

	private static TradeOrder order(String security) {
		return new TradeOrder(null, 22214, security, TradeSide.Buy, 100);
	}
}
