package finos.traderx.tradeservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeservice.exceptions.CreditLimitExceededException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Position;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class CreditCheckServiceTest {

	private static final String ACCOUNT_URL = "http://account-service";
	private static final String POSITION_URL = "http://position-service";
	private static final int ACCOUNT_ID = 22214;

	private RestTemplate restTemplate;
	private CreditCheckService creditCheckService;

	@BeforeEach
	void setUp() {
		restTemplate = mock(RestTemplate.class);
		creditCheckService = new CreditCheckService(restTemplate, ACCOUNT_URL, POSITION_URL);
	}

	private void stubAccount(Long creditLimit) {
		Account account = new Account(ACCOUNT_ID, "Test Account 20", creditLimit);
		when(restTemplate.getForEntity(contains("/account/" + ACCOUNT_ID), eq(Account.class)))
				.thenReturn(ResponseEntity.ok(account));
	}

	private void stubPositions(Position... positions) {
		when(restTemplate.getForEntity(contains("/positions/" + ACCOUNT_ID), eq(Position[].class)))
				.thenReturn(ResponseEntity.ok(positions));
	}

	private TradeOrder buyOrder(String security, int quantity) {
		return new TradeOrder("TRADE-1", ACCOUNT_ID, security, TradeSide.Buy, quantity);
	}

	@Test
	void allowsTradeUnderCreditLimit() {
		// Existing gross exposure: |1000| + |-100| = 1100. Buy 500 IBM -> IBM net 400.
		// Post-trade gross: |1000| + |400| = 1400, under limit.
		stubAccount(10000L);
		stubPositions(
				new Position(ACCOUNT_ID, "MS", 1000),
				new Position(ACCOUNT_ID, "IBM", -100));

		TradeOrder order = buyOrder("IBM", 500);

		assertEquals(1400L, creditCheckService.computePostTradeExposure(order));
		assertDoesNotThrow(() -> creditCheckService.assertWithinCreditLimit(order));
	}

	@Test
	void allowsTradeExactlyAtCreditLimit() {
		// Post-trade gross exposure lands exactly on the limit and must be accepted.
		stubAccount(1400L);
		stubPositions(
				new Position(ACCOUNT_ID, "MS", 1000),
				new Position(ACCOUNT_ID, "IBM", -100));

		TradeOrder order = buyOrder("IBM", 500);

		assertEquals(1400L, creditCheckService.computePostTradeExposure(order));
		assertDoesNotThrow(() -> creditCheckService.assertWithinCreditLimit(order));
	}

	@Test
	void rejectsTradeExceedingCreditLimit() {
		// Post-trade gross exposure (1400) exceeds the limit (1399).
		stubAccount(1399L);
		stubPositions(
				new Position(ACCOUNT_ID, "MS", 1000),
				new Position(ACCOUNT_ID, "IBM", -100));

		TradeOrder order = buyOrder("IBM", 500);

		assertEquals(1400L, creditCheckService.computePostTradeExposure(order));
		CreditLimitExceededException ex = assertThrows(CreditLimitExceededException.class,
				() -> creditCheckService.assertWithinCreditLimit(order));
		assertEquals(true, ex.getMessage().contains("exceeds credit limit"));
	}

	@Test
	void sellIncreasesShortExposure() {
		// Selling into no position creates a short; gross exposure uses absolute value.
		stubAccount(100000L);
		stubPositions();

		TradeOrder sell = new TradeOrder("TRADE-2", ACCOUNT_ID, "IBM", TradeSide.Sell, 2000);

		assertEquals(2000L, creditCheckService.computePostTradeExposure(sell));
	}

	@Test
	void skipsCheckWhenNoCreditLimitConfigured() {
		stubAccount(null);
		stubPositions(new Position(ACCOUNT_ID, "MS", 1000));

		TradeOrder order = buyOrder("MS", 5000);

		assertDoesNotThrow(() -> creditCheckService.assertWithinCreditLimit(order));
	}
}
