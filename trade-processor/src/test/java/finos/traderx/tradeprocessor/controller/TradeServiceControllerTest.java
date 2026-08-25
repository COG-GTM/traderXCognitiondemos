package finos.traderx.tradeprocessor.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;
import finos.traderx.tradeprocessor.service.TradeService;

/**
 * API surface of POST /tradeservice/order.
 */
@WebMvcTest(TradeServiceController.class)
class TradeServiceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	TradeService tradeService;

	private TradeBookingResult booking(Integer accountId, String security, int quantity) {
		Trade t = new Trade();
		t.setId("trade-1");
		t.setAccountId(accountId);
		t.setSecurity(security);
		t.setSide(TradeSide.Buy);
		t.setQuantity(quantity);
		t.setState(TradeState.Settled);
		t.setCreated(new Date());
		t.setUpdated(new Date());
		Position p = new Position();
		p.setAccountId(accountId);
		p.setSecurity(security);
		p.setQuantity(quantity);
		return new TradeBookingResult(t, p);
	}

	/** API-01 */
	@Test
	@DisplayName("API-01 happy path returns 200 and a TradeBookingResult")
	void happyPath() throws Exception {
		when(tradeService.processTrade(any(TradeOrder.class))).thenReturn(booking(1, "IBM", 10));

		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"o-1\",\"accountId\":1,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trade.id").value("trade-1"))
				.andExpect(jsonPath("$.trade.state").value("Settled"))
				.andExpect(jsonPath("$.position.quantity").value(10));
	}

	/** API-02 */
	@Test
	@DisplayName("API-02 malformed JSON is rejected with 400 and never reaches the service")
	void malformedJsonIsRejected() throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":1,")).andExpect(status().isBadRequest());
		verify(tradeService, never()).processTrade(any(TradeOrder.class));
	}

	/** API-03 */
	@Test
	@DisplayName("API-03 an empty body is rejected with 400")
	void emptyBodyIsRejected() throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON).content(""))
				.andExpect(status().isBadRequest());
		verify(tradeService, never()).processTrade(any(TradeOrder.class));
	}

	/** API-04 */
	@Test
	@DisplayName("API-04 an unknown side value is rejected with 400")
	void unknownSideIsRejected() throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":1,\"security\":\"IBM\",\"side\":\"Short\",\"quantity\":10}"))
				.andExpect(status().isBadRequest());
		verify(tradeService, never()).processTrade(any(TradeOrder.class));
	}

	/** API-05 */
	@Test
	@DisplayName("API-05 an order with every field missing is accepted: there is no request validation")
	void missingFieldsAreAccepted() throws Exception {
		when(tradeService.processTrade(any(TradeOrder.class))).thenReturn(booking(null, null, 0));

		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk());

		org.mockito.ArgumentCaptor<TradeOrder> captor = org.mockito.ArgumentCaptor.forClass(TradeOrder.class);
		verify(tradeService).processTrade(captor.capture());
		assertNull(captor.getValue().getAccountId());
		assertNull(captor.getValue().getSecurity());
		assertNull(captor.getValue().getSide());
		assertNull(captor.getValue().getQuantity());
	}

	/** API-06 */
	@Test
	@DisplayName("API-06 an unknown field in the body is ignored rather than rejected")
	void unknownFieldIsIgnored() throws Exception {
		when(tradeService.processTrade(any(TradeOrder.class))).thenReturn(booking(1, "IBM", 10));

		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":1,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":10,\"price\":999}"))
				.andExpect(status().isOk());
	}

	/** API-07 */
	@Test
	@DisplayName("API-07 the endpoint is unauthenticated and books trades for a non-existent account and ticker")
	void endpointIsUnauthenticatedAndUnvalidated() throws Exception {
		when(tradeService.processTrade(any(TradeOrder.class))).thenReturn(booking(999999, "NOTATICKER", 10));

		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":999999,\"security\":\"NOTATICKER\",\"side\":\"Buy\",\"quantity\":10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position.security").value("NOTATICKER"));

		// No authentication or authorization metadata anywhere on the endpoint...
		for (java.lang.annotation.Annotation a : TradeServiceController.class.getAnnotations()) {
			assertTrue(!a.annotationType().getName().contains("security"),
					"unexpected security annotation " + a);
		}
		assertEquals("*", TradeServiceController.class
				.getAnnotation(org.springframework.web.bind.annotation.CrossOrigin.class).value()[0],
				"CORS is wide open");

		// ...and the processor never calls reference-data or account-service:
		// the trade-service validations are bypassed entirely by this endpoint.
		for (Field f : TradeService.class.getDeclaredFields()) {
			String type = f.getType().getSimpleName();
			assertTrue(!type.contains("RestTemplate") && !type.contains("WebClient"),
					"unexpected validation client " + f);
		}
	}
}
