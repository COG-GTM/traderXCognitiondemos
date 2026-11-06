package finos.traderx.tradeservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

class TradeOrderControllerValidationTest {

	private Publisher<TradeOrder> publisher;
	private MockMvc mvc;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		publisher = mock(Publisher.class);
		TradeOrderController controller = new TradeOrderController();
		ReflectionTestUtils.setField(controller, "tradePublisher", publisher);
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"{\"accountId\":22214,\"security\":\"MSFT\",\"side\":\"Buy\"}",
		"{\"accountId\":22214,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":0}",
		"{\"accountId\":22214,\"security\":\"MSFT\",\"side\":\"Sell\",\"quantity\":-1000000}",
		"{\"accountId\":22214,\"security\":\"MSFT\",\"quantity\":10}",
		"{\"accountId\":22214,\"security\":\"MSFT\",\"side\":\"Short\",\"quantity\":10}",
		"{\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":10}",
		"{\"accountId\":22214,\"side\":\"Buy\",\"quantity\":10}"
	})
	void rejectsInvalidOrdersBeforePublishing(String body) throws Exception {
		mvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
		verify(publisher, never()).publish(anyString(), any());
	}
}
