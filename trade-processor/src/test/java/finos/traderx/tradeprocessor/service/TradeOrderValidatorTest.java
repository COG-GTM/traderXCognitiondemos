package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.exceptions.TradeValidationUnavailableException;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;

class TradeOrderValidatorTest {

	private static final String REF = "http://reference-data:18085";
	private static final String ACCOUNTS = "http://account-service:18088";

	private MockRestServiceServer server;
	private TradeOrderValidator validator;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		validator = new TradeOrderValidator(restTemplate, REF + "/", ACCOUNTS, 1_000_000);
	}

	private static TradeOrder order(Integer accountId, String security, TradeSide side, Integer quantity) {
		TradeOrder o = new TradeOrder();
		org.springframework.test.util.ReflectionTestUtils.setField(o, "accountId", accountId);
		org.springframework.test.util.ReflectionTestUtils.setField(o, "security", security);
		org.springframework.test.util.ReflectionTestUtils.setField(o, "side", side);
		org.springframework.test.util.ReflectionTestUtils.setField(o, "quantity", quantity);
		return o;
	}

	private void stockExists(String ticker) {
		server.expect(requestTo(REF + "/stocks/" + ticker))
				.andRespond(withSuccess("{\"ticker\":\"" + ticker + "\"}", MediaType.APPLICATION_JSON));
	}

	private void accountExists(int id) {
		server.expect(requestTo(ACCOUNTS + "/account/" + id))
				.andRespond(withSuccess("{\"id\":" + id + "}", MediaType.APPLICATION_JSON));
	}

	@Test
	void acceptsValidOrder() {
		stockExists("BRK.B");
		accountExists(22214);
		assertDoesNotThrow(() -> validator.validate(order(22214, "BRK.B", TradeSide.Buy, 100)));
		server.verify();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1, Integer.MIN_VALUE, 1_000_001, Integer.MAX_VALUE })
	void rejectsOutOfRangeQuantityWithoutCallingServices(int quantity) {
		assertThrows(InvalidTradeOrderException.class,
				() -> validator.validate(order(22214, "IBM", TradeSide.Buy, quantity)));
		server.verify();
	}

	@Test
	void rejectsMissingFields() {
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(null));
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(order(null, "IBM", TradeSide.Buy, 1)));
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(order(-5, "IBM", TradeSide.Buy, 1)));
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(order(22214, null, TradeSide.Buy, 1)));
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(order(22214, "IBM", null, 1)));
		assertThrows(InvalidTradeOrderException.class, () -> validator.validate(order(22214, "IBM", TradeSide.Sell, null)));
		server.verify();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "X Y", "../account/1", "IBM?x=1", "A%2F", "TOOLONGTICKER" })
	void rejectsMalformedTicker(String ticker) {
		assertThrows(InvalidTradeOrderException.class,
				() -> validator.validate(order(22214, ticker, TradeSide.Buy, 1)));
		server.verify();
	}

	@Test
	void rejectsUnknownSecurity() {
		server.expect(requestTo(REF + "/stocks/X")).andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertThrows(InvalidTradeOrderException.class,
				() -> validator.validate(order(22214, "X", TradeSide.Buy, 1)));
		server.verify();
	}

	@Test
	void rejectsUnknownAccount() {
		stockExists("IBM");
		server.expect(requestTo(ACCOUNTS + "/account/999999")).andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertThrows(InvalidTradeOrderException.class,
				() -> validator.validate(order(999999, "IBM", TradeSide.Buy, 1)));
		server.verify();
	}

	@Test
	void failsClosedWhenReferenceDataIsDown() {
		server.expect(requestTo(REF + "/stocks/IBM")).andRespond(withServerError());
		assertThrows(TradeValidationUnavailableException.class,
				() -> validator.validate(order(22214, "IBM", TradeSide.Buy, 1)));
		server.verify();
	}
}
