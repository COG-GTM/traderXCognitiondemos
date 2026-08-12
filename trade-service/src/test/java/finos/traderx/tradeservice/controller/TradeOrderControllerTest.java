package finos.traderx.tradeservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.risk.RiskLimitProperties;
import finos.traderx.tradeservice.risk.RiskLimitService;
import finos.traderx.tradeservice.service.AccountValidationService;
import finos.traderx.tradeservice.service.ReferenceDataService;

class TradeOrderControllerTest {

	private static final String ORDER_JSON =
		"{\"id\":\"order-1\",\"accountId\":42,\"security\":\"AAPL\",\"side\":\"Buy\",\"quantity\":11}";

	@SuppressWarnings("unchecked")
	private final Publisher<TradeOrder> publisher = mock(Publisher.class);
	private final ReferenceDataService referenceDataService = mock(ReferenceDataService.class);
	private final AccountValidationService accountValidationService = mock(AccountValidationService.class);

	private RiskLimitProperties properties;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		properties = new RiskLimitProperties();
		properties.getNotionalLimit().setDefaultLimit(new BigDecimal("1000"));

		TradeOrderController controller = new TradeOrderController();
		ReflectionTestUtils.setField(controller, "tradePublisher", publisher);
		ReflectionTestUtils.setField(controller, "referenceDataService", referenceDataService);
		ReflectionTestUtils.setField(controller, "accountValidationService", accountValidationService);
		ReflectionTestUtils.setField(controller, "riskLimitService", new RiskLimitService(properties));

		when(referenceDataService.findSecurity("AAPL"))
			.thenReturn(Optional.of(new Security("AAPL", "Apple", new BigDecimal("100"))));
		when(accountValidationService.accountExists(42)).thenReturn(true);

		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void breachReturns422WithStructuredBody() throws Exception {
		mockMvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.decision").value("REJECTED"))
			.andExpect(jsonPath("$.reason").value("NOTIONAL_LIMIT_BREACH"))
			.andExpect(jsonPath("$.limit").value(1000))
			.andExpect(jsonPath("$.attempted").value(1100));

		verify(publisher, never()).publish(any(), any());
	}

	@Test
	void orderWithinLimitStillBooks() throws Exception {
		properties.getNotionalLimit().setDefaultLimit(new BigDecimal("100000"));

		mockMvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.security").value("AAPL"));

		verify(publisher).publish(any(), any());
	}
}
