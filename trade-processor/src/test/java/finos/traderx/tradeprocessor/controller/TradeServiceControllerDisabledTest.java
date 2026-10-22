package finos.traderx.tradeprocessor.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.tradeprocessor.service.TradeService;

@WebMvcTest(TradeServiceController.class)
class TradeServiceControllerDisabledTest {

	static final String ORDER = "{\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":100}";
	static final String HOSTILE_ORDER = "{\"accountId\":22214,\"security\":\"X\",\"side\":\"Buy\",\"quantity\":-2147483648}";

	@Autowired
	MockMvc mvc;

	@MockBean
	TradeService tradeService;

	@Test
	void endpointIsDisabled() throws Exception {
		mvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON).content(HOSTILE_ORDER))
				.andExpect(status().isNotFound());
		verifyNoInteractions(tradeService);
	}
}
