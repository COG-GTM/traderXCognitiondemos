package finos.traderx.positionservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import finos.traderx.positionservice.model.Trade;
import finos.traderx.positionservice.service.TradeService;

/** Edge and corner case coverage for the read-only /trades endpoints. */
@WebMvcTest(TradeController.class)
class TradeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TradeService tradeService;

	private static Trade trade(String id, int accountId, String security, String side, int quantity) {
		Trade trade = new Trade();
		trade.setId(id);
		trade.setAccountId(accountId);
		trade.setSecurity(security);
		trade.setSide(side);
		trade.setQuantity(quantity);
		trade.setCreated(new Date(0));
		trade.setUpdated(new Date(0));
		return trade;
	}

	@Test
	@DisplayName("PS-25f: GET /trades/{accountId} for an unknown account returns 200 with an empty array")
	void unknownAccountReturnsEmptyArray() throws Exception {
		when(this.tradeService.getTradesByAccountID(987654)).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/trades/987654")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-25g: GET /trades/-1 and /trades/0 return 200 with an empty array")
	void boundaryAccountIdsReturnEmptyArray() throws Exception {
		when(this.tradeService.getTradesByAccountID(-1)).thenReturn(Collections.emptyList());
		when(this.tradeService.getTradesByAccountID(0)).thenReturn(Collections.emptyList());

		assertEquals("[]", this.mockMvc.perform(get("/trades/-1")).andReturn().getResponse().getContentAsString());
		assertEquals("[]", this.mockMvc.perform(get("/trades/0")).andReturn().getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-25h: GET /trades/{accountId} with a non-numeric id returns 500 (not 400)")
	void nonNumericAccountIdReturns500() throws Exception {
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				this.mockMvc.perform(get("/trades/abc")).andReturn().getResponse().getStatus());
	}

	@Test
	@Disabled("LATENT BUG: TradeController's @ExceptionHandler(Exception.class) catches "
			+ "MethodArgumentTypeMismatchException, so a malformed accountId is a 500 instead of a 400")
	@DisplayName("PS-25i: GET /trades/{accountId} with a non-numeric id should return 400")
	void nonNumericAccountIdShouldReturn400() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				this.mockMvc.perform(get("/trades/abc")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("PS-26c: GET /trades/ on an empty repository returns an empty array, not null")
	void emptyRepositoryReturnsEmptyArray() throws Exception {
		when(this.tradeService.getAllTrades()).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/trades/")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-26d: GET /trades (no trailing slash) does not match the '/' mapping")
	void listWithoutTrailingSlashIsNotMapped() throws Exception {
		assertEquals(HttpStatus.NOT_FOUND.value(),
				this.mockMvc.perform(get("/trades")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("PS-27c: a repository failure returns a 500 whose body is the raw exception message")
	void repositoryFailureLeaksMessage() throws Exception {
		when(this.tradeService.getAllTrades())
				.thenThrow(new RuntimeException("JDBC exception executing SQL [select t1_0.ID from TRADES t1_0]"));

		MvcResult result = this.mockMvc.perform(get("/trades/")).andReturn();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertEquals("JDBC exception executing SQL [select t1_0.ID from TRADES t1_0]",
				result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: TradeController.generalError returns e.getMessage() verbatim, leaking SQL and "
			+ "infrastructure detail to the caller (information disclosure)")
	@DisplayName("PS-27d: a repository failure should not echo the internal exception message")
	void repositoryFailureShouldNotLeakMessage() throws Exception {
		when(this.tradeService.getAllTrades())
				.thenThrow(new RuntimeException("JDBC exception executing SQL [select t1_0.ID from TRADES t1_0]"));

		assertTrue(!this.mockMvc.perform(get("/trades/")).andReturn().getResponse()
				.getContentAsString().contains("select"), "internal detail leaked");
	}

	@Test
	@DisplayName("PS-28d: trades with zero and negative quantities are returned unchanged")
	void zeroAndNegativeQuantityTradesAreReturned() throws Exception {
		when(this.tradeService.getTradesByAccountID(1)).thenReturn(List.of(
				trade("t1", 1, "AAPL", "Buy", 0),
				trade("t2", 1, "AAPL", "Sell", -100)));

		String body = this.mockMvc.perform(get("/trades/1")).andReturn().getResponse().getContentAsString();

		assertTrue(body.contains("\"quantity\":0"), "body: " + body);
		assertTrue(body.contains("\"quantity\":-100"), "body: " + body);
	}

	@Test
	@DisplayName("PS-28e: a trade keeps its default 'UNSET' state when nothing set it")
	void defaultStateIsUnset() throws Exception {
		Trade trade = trade("t3", 1, "AAPL", "Buy", 10);
		when(this.tradeService.getTradesByAccountID(1)).thenReturn(List.of(trade));

		String body = this.mockMvc.perform(get("/trades/1")).andReturn().getResponse().getContentAsString();

		assertTrue(body.contains("\"state\":\"UNSET\""), "body: " + body);
	}

	@Test
	@DisplayName("PS-28f: an oversized side value is serialised as-is (no API-level length check)")
	void oversizedSideIsReturned() throws Exception {
		when(this.tradeService.getTradesByAccountID(anyInt()))
				.thenReturn(List.of(trade("t4", 1, "AAPL", "ShortSell", 10)));

		String body = this.mockMvc.perform(get("/trades/1")).andReturn().getResponse().getContentAsString();

		// SIDE is a 4 char column, but nothing on the read path validates it.
		assertTrue(body.contains("\"side\":\"ShortSell\""), "body: " + body);
	}
}
