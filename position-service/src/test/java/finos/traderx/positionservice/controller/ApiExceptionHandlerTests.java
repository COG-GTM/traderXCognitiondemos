package finos.traderx.positionservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import finos.traderx.positionservice.service.PositionService;
import finos.traderx.positionservice.service.TradeService;

class ApiExceptionHandlerTests {

	private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

	private PositionService positionService;
	private TradeService tradeService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		positionService = mock(PositionService.class);
		tradeService = mock(TradeService.class);
		PositionController positionController = new PositionController();
		positionController.positionService = positionService;
		TradeController tradeController = new TradeController();
		tradeController.tradeService = tradeService;
		mockMvc = MockMvcBuilders.standaloneSetup(positionController, tradeController)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void positionsWithNonNumericAccountIdReturnsJsonWithoutEchoingInput() throws Exception {
		mockMvc.perform(get("/positions/{id}", "<h1>Accountfrozen.Call555-0199<h1>").header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string(not(containsString("<h1>"))))
				.andExpect(content().string(not(containsString("Accountfrozen"))))
				.andExpect(jsonPath("$.error").value("Invalid value for parameter 'accountId'"));
	}

	@Test
	void tradesWithNonNumericAccountIdReturnsJsonWithoutEchoingInput() throws Exception {
		mockMvc.perform(get("/trades/{id}", "<b>x<b>").header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string(not(containsString("<b>"))));
	}

	@Test
	void unexpectedErrorReturnsFixedMessageWithoutInternalDetails() throws Exception {
		when(positionService.getPositionsByAccountID(22214))
				.thenThrow(new IllegalStateException("Connection refused: db-host:18082 <script>"));

		mockMvc.perform(get("/positions/22214").header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string(not(containsString("db-host"))))
				.andExpect(content().string(not(containsString("<script>"))))
				.andExpect(jsonPath("$.error").value("Internal server error"));
	}

	@Test
	void unexpectedTradeErrorReturnsFixedMessage() throws Exception {
		when(tradeService.getAllTrades()).thenThrow(new RuntimeException("jdbc:h2:tcp://db-host:18082/traderx"));

		mockMvc.perform(get("/trades/").header("Accept", BROWSER_ACCEPT))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string(not(containsString("jdbc"))))
				.andExpect(jsonPath("$.error").value("Internal server error"));
	}
}
