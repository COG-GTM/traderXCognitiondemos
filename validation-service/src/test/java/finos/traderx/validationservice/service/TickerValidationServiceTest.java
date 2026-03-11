package finos.traderx.validationservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.validationservice.model.Security;

@ExtendWith(MockitoExtension.class)
class TickerValidationServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private TickerValidationService tickerValidationService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(tickerValidationService, "referenceDataServiceAddress", "http://localhost:18085");
	}

	@Test
	void validateTicker_validTicker_returnsTrue() {
		Security security = new Security("AAPL", "Apple Inc.");
		ResponseEntity<Security> response = new ResponseEntity<>(security, HttpStatus.OK);
		when(restTemplate.getForEntity("http://localhost:18085//stocks/AAPL", Security.class))
				.thenReturn(response);

		boolean result = tickerValidationService.validateTicker("AAPL");

		assertTrue(result);
	}

	@Test
	void validateTicker_invalidTicker_returnsFalse() {
		when(restTemplate.getForEntity("http://localhost:18085//stocks/INVALID", Security.class))
				.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

		boolean result = tickerValidationService.validateTicker("INVALID");

		assertFalse(result);
	}
}
