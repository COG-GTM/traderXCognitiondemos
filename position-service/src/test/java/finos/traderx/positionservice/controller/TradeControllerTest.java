package finos.traderx.positionservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import finos.traderx.positionservice.model.Trade;
import finos.traderx.positionservice.service.TradeService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	TradeService tradeService;

	private Trade trade(String id, Integer accountId, String security, String side, Integer quantity) {
		Trade trade = new Trade();
		trade.setId(id);
		trade.setAccountId(accountId);
		trade.setSecurity(security);
		trade.setSide(side);
		trade.setQuantity(quantity);
		return trade;
	}

	@Test
	@DisplayName("GET /trades/{accountId} returns the trades of the account")
	void getByAccountIdReturnsTrades() throws Exception {
		when(this.tradeService.getTradesByAccountID(1)).thenReturn(Arrays.asList(
				trade("t1", 1, "MSFT", "Buy", 100),
				trade("t2", 1, "AAPL", "Sell", 25)));

		this.mockMvc.perform(get("/trades/{accountId}", 1))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value("t1"))
				.andExpect(jsonPath("$[1].side").value("Sell"));
	}

	@Test
	@DisplayName("GET /trades/{accountId} returns an empty array for an account without trades")
	void getByAccountIdReturnsEmptyArray() throws Exception {
		when(this.tradeService.getTradesByAccountID(404)).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/trades/{accountId}", 404))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("GET /trades/{accountId} with a non-numeric account id never reaches the service")
	void getByAccountIdRejectsNonNumericAccountId() throws Exception {
		this.mockMvc.perform(get("/trades/{accountId}", "not-a-number"))
				.andExpect(status().isInternalServerError());

		verify(this.tradeService, never()).getTradesByAccountID(anyInt());
	}

	@Test
	@DisplayName("GET /trades/ returns all trades with their default state")
	void getAllTradesReturnsTrades() throws Exception {
		when(this.tradeService.getAllTrades()).thenReturn(Collections.singletonList(new Trade()));

		this.mockMvc.perform(get("/trades/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].state").value("UNSET"));
	}

	@Test
	@DisplayName("GET /trades/ returns an empty array when no trades exist")
	void getAllTradesReturnsEmptyArray() throws Exception {
		when(this.tradeService.getAllTrades()).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/trades/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Unexpected service failures are mapped to 500")
	void unexpectedFailureIsMappedToInternalServerError() throws Exception {
		when(this.tradeService.getAllTrades()).thenThrow(new IllegalStateException("database unavailable"));

		this.mockMvc.perform(get("/trades/"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string("database unavailable"));
	}
}
