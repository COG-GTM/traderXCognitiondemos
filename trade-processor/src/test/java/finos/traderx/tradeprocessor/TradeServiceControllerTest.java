package finos.traderx.tradeprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/test-application.properties")
class TradeServiceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@BeforeEach
	void reset() {
		tradeRepository.deleteAll();
		positionRepository.deleteAll();
	}

	@Test
	void buyThenSellNetsPosition() throws Exception {
		mockMvc.perform(post("/tradeservice/order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22214,\"security\":\"MSFT\",\"quantity\":100,\"side\":\"Buy\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trade.state").value("Settled"))
			.andExpect(jsonPath("$.trade.side").value("Buy"))
			.andExpect(jsonPath("$.trade.id").isString())
			.andExpect(jsonPath("$.position.quantity").value(100));

		mockMvc.perform(post("/tradeservice/order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22214,\"security\":\"MSFT\",\"quantity\":40,\"side\":\"Sell\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trade.state").value("Settled"))
			.andExpect(jsonPath("$.position.accountId").value(22214))
			.andExpect(jsonPath("$.position.security").value("MSFT"))
			.andExpect(jsonPath("$.position.quantity").value(60));

		assertEquals(2, tradeRepository.count());
		assertEquals(60, positionRepository.findByAccountIdAndSecurity(22214, "MSFT").getQuantity());
	}

	@Test
	void malformedOrderIsRejected() throws Exception {
		mockMvc.perform(post("/tradeservice/order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not json"))
			.andExpect(status().isBadRequest());
	}
}
