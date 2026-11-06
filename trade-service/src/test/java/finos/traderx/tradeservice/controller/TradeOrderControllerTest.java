package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class TradeOrderControllerTest {

	private final List<TradeOrder> published = new ArrayList<>();
	private TradeOrderController controller;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		controller = new TradeOrderController();
		ReflectionTestUtils.setField(controller, "tradePublisher", new Publisher<TradeOrder>() {
			public void publish(TradeOrder message) { published.add(message); }
			public void publish(String topic, TradeOrder message) { published.add(message); }
			public boolean isConnected() { return true; }
			public void connect() {}
			public void disconnect() {}
		});
		ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", "http://reference-data");
		ReflectionTestUtils.setField(controller, "accountServiceAddress", "http://account-service");
		ReflectionTestUtils.setField(controller, "maxOrderQuantity", 1_000_000);
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
		server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
	}

	@Test
	void acceptedOrdersGetAUniqueServerAssignedId() {
		server.expect(manyTimes(), requestTo("http://reference-data/stocks/IBM"))
				.andRespond(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}", MediaType.APPLICATION_JSON));
		server.expect(manyTimes(), requestTo("http://account-service/account/22214"))
				.andRespond(withSuccess("{\"id\":22214,\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));

		String first = controller.createTradeOrder(new TradeOrder("TRADE-1", 22214, "IBM", TradeSide.Buy, 10)).getBody().getId();
		String second = controller.createTradeOrder(new TradeOrder("TRADE-1", 22214, "IBM", TradeSide.Buy, 10)).getBody().getId();

		UUID.fromString(first);
		UUID.fromString(second);
		assertNotEquals(first, second);
		assertEquals(2, published.size());
		assertEquals(first, published.get(0).getId());
		assertEquals(second, published.get(1).getId());
	}

	@Test
	void ordersOutsideTheQuantityLimitOrWithoutSideAreRejected() {
		assertThrows(InvalidTradeOrderException.class,
				() -> controller.createTradeOrder(new TradeOrder(null, 22214, "IBM", TradeSide.Buy, Integer.MAX_VALUE)));
		assertThrows(InvalidTradeOrderException.class,
				() -> controller.createTradeOrder(new TradeOrder(null, 22214, "IBM", TradeSide.Buy, 1_000_001)));
		assertThrows(InvalidTradeOrderException.class,
				() -> controller.createTradeOrder(new TradeOrder(null, 22214, "IBM", TradeSide.Sell, 0)));
		assertThrows(InvalidTradeOrderException.class,
				() -> controller.createTradeOrder(new TradeOrder(null, 22214, "IBM", null, 10)));

		assertTrue(published.isEmpty());
		server.verify();
	}
}
