package finos.traderx.tradeprocessor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.tradeprocessor.exception.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.service.TradeService;

@WebMvcTest(TradeServiceController.class)
class TradeServiceControllerTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	TradeService tradeService;

	@Test
	void invalidOrderReturnsBadRequest() throws Exception {
		when(tradeService.processTrade(any())).thenThrow(new InvalidTradeOrderException("bad quantity"));

		mvc.perform(post("/tradeservice/order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":-1000}"))
				.andExpect(status().isBadRequest());
	}
}
