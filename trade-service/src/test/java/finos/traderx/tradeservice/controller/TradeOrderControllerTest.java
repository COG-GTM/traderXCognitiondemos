package finos.traderx.tradeservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

@WebMvcTest(TradeOrderController.class)
class TradeOrderControllerTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	Publisher<TradeOrder> tradePublisher;

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":-1000}",
			"{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":0}",
			"{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":2147483647}",
			"{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\"}",
			"{\"accountId\":22214,\"security\":\"IBM\",\"quantity\":10}"
	})
	void rejectsInvalidOrdersWithoutPublishing(String body) throws Exception {
		mvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());

		verify(tradePublisher, never()).publish(anyString(), any());
	}
}
