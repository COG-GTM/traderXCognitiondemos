package finos.traderx.tradeprocessor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;
import finos.traderx.tradeprocessor.service.TradeService;

@WebMvcTest(TradeServiceController.class)
@Import(TradeService.class)
class TradeServiceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	TradeRepository tradeRepository;

	@MockBean
	PositionRepository positionRepository;

	@MockBean
	Publisher<Trade> tradePublisher;

	@MockBean
	Publisher<Position> positionPublisher;

	private void expectBadRequest(String body) throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void invalidOrdersReturn400AndAreNotBooked() throws Exception {
		expectBadRequest("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":-2147483000}");
		expectBadRequest("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":0}");
		expectBadRequest("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\"}");
		expectBadRequest("{\"accountId\":22,\"security\":\"MSFT\",\"quantity\":100}");
		verify(tradeRepository, never()).save(any());
		verify(positionRepository, never()).save(any());
	}

	@Test
	void validOrderIsBooked() throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":100}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position.quantity").value(100))
				.andExpect(jsonPath("$.trade.side").value("Buy"));
	}
}
