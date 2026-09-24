package finos.traderx.tradeservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class TradeOrderJsonTest {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.setSerializationInclusion(JsonInclude.Include.NON_NULL)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	@Test
	void deserializesEveryFieldOfAnIncomingOrder() throws Exception {
		String json = """
				{
				  "id": "trade-1",
				  "state": "New",
				  "security": "IBM",
				  "quantity": 10,
				  "accountId": 1,
				  "side": "Buy"
				}
				""";

		TradeOrder order = this.objectMapper.readValue(json, TradeOrder.class);

		assertThat(order).isEqualTo(new TradeOrder("trade-1", "New", "IBM", 10, 1, TradeSide.Buy));
	}

	@Test
	void deserializesOrderSubmittedByTheUiWithoutIdOrState() throws Exception {
		String json = """
				{"security":"MSFT","quantity":5,"accountId":2,"side":"Sell"}
				""";

		TradeOrder order = this.objectMapper.readValue(json, TradeOrder.class);

		assertThat(order.security()).isEqualTo("MSFT");
		assertThat(order.quantity()).isEqualTo(5);
		assertThat(order.accountId()).isEqualTo(2);
		assertThat(order.side()).isEqualTo(TradeSide.Sell);
		assertThat(order.id()).isNull();
		assertThat(order.state()).isNull();
	}

	@Test
	void serializesWithTheFieldNamesConsumedByTheTradeFeed() throws Exception {
		String json = this.objectMapper.writeValueAsString(new TradeOrder("trade-1", 1, "IBM", TradeSide.Buy, 10));

		assertThat(json).isEqualTo("{\"id\":\"trade-1\",\"security\":\"IBM\",\"quantity\":10,\"accountId\":1,"
				+ "\"side\":\"Buy\"}");
	}
}
