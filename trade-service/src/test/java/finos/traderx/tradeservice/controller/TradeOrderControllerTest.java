package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import finos.traderx.tradeservice.model.ValidationResult;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerTest {

	@Mock
	private Publisher<TradeOrder> tradePublisher;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private TradeOrderController tradeOrderController;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(tradeOrderController, "validationServiceAddress", "http://localhost:18094");
	}

	@Test
	void createTradeOrder_validOrder_returnsOk() throws Exception {
		TradeOrder tradeOrder = new TradeOrder("order1", 12345, "AAPL", TradeSide.Buy, 100);

		ValidationResult validResult = new ValidationResult();
		validResult.setValid(true);
		validResult.setErrors(new ArrayList<>());

		when(restTemplate.postForObject(
				eq("http://localhost:18094/validate/trade-order"),
				any(Map.class),
				eq(ValidationResult.class)))
				.thenReturn(validResult);

		ResponseEntity<TradeOrder> response = tradeOrderController.createTradeOrder(tradeOrder);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(tradeOrder, response.getBody());
	}

	@Test
	void createTradeOrder_invalidOrder_throwsException() {
		TradeOrder tradeOrder = new TradeOrder("order2", 99999, "INVALID", TradeSide.Sell, 50);

		ValidationResult invalidResult = new ValidationResult();
		invalidResult.setValid(false);
		invalidResult.setErrors(Arrays.asList("INVALID not found in Reference data service."));

		when(restTemplate.postForObject(
				eq("http://localhost:18094/validate/trade-order"),
				any(Map.class),
				eq(ValidationResult.class)))
				.thenReturn(invalidResult);

		assertThrows(ResourceNotFoundException.class, () -> {
			tradeOrderController.createTradeOrder(tradeOrder);
		});
	}
}
