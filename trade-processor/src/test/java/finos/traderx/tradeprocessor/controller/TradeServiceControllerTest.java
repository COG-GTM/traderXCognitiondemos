package finos.traderx.tradeprocessor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.service.TradeService;

@WebMvcTest(TradeServiceController.class)
@TestPropertySource(properties = "trade.processor.order.token=test-service-token")
class TradeServiceControllerTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	TradeService tradeService;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON).content(TradeServiceControllerDisabledTest.HOSTILE_ORDER))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(tradeService);
	}

	@Test
	void rejectsWrongToken() throws Exception {
		mvc.perform(post("/tradeservice/order").header(HttpHeaders.AUTHORIZATION, "Bearer nope")
				.contentType(MediaType.APPLICATION_JSON).content(TradeServiceControllerDisabledTest.ORDER))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(tradeService);
	}

	@Test
	void acceptsServiceToken() throws Exception {
		mvc.perform(post("/tradeservice/order").header(HttpHeaders.AUTHORIZATION, "Bearer test-service-token")
				.contentType(MediaType.APPLICATION_JSON).content(TradeServiceControllerDisabledTest.ORDER))
				.andExpect(status().isOk());
	}

	@Test
	void invalidOrderFromServiceCallerIsBadRequest() throws Exception {
		when(tradeService.processTrade(any())).thenThrow(new InvalidTradeOrderException("quantity"));
		mvc.perform(post("/tradeservice/order").header(HttpHeaders.AUTHORIZATION, "Bearer test-service-token")
				.contentType(MediaType.APPLICATION_JSON).content(TradeServiceControllerDisabledTest.HOSTILE_ORDER))
				.andExpect(status().isBadRequest());
	}

	@Test
	void noWildcardCors() throws Exception {
		mvc.perform(options("/tradeservice/order").header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
