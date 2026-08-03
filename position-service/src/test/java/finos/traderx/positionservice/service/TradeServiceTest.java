package finos.traderx.positionservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import finos.traderx.positionservice.model.Trade;
import finos.traderx.positionservice.repository.TradeRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	@Mock
	TradeRepository tradeRepository;

	@InjectMocks
	TradeService tradeService;

	private Trade trade(String id, Integer accountId, String security, String side, Integer quantity) {
		Trade trade = new Trade();
		trade.setId(id);
		trade.setAccountId(accountId);
		trade.setSecurity(security);
		trade.setSide(side);
		trade.setQuantity(quantity);
		return trade;
	}

	@Test
	@DisplayName("getAllTrades returns every trade provided by the repository")
	void getAllTradesReturnsAllTrades() {
		when(this.tradeRepository.findAll()).thenReturn(Arrays.asList(
				trade("t1", 1, "MSFT", "Buy", 100),
				trade("t2", 2, "AAPL", "Sell", 50)));

		List<Trade> trades = this.tradeService.getAllTrades();

		assertEquals(2, trades.size());
		assertEquals("t1", trades.get(0).getId());
	}

	@Test
	@DisplayName("getAllTrades returns an empty list when no trades exist")
	void getAllTradesReturnsEmptyList() {
		when(this.tradeRepository.findAll()).thenReturn(Collections.emptyList());

		assertTrue(this.tradeService.getAllTrades().isEmpty());
	}

	@Test
	@DisplayName("getTradesByAccountID returns only the trades of the requested account")
	void getTradesByAccountIdReturnsAccountTrades() {
		when(this.tradeRepository.findByAccountId(1)).thenReturn(Arrays.asList(
				trade("t1", 1, "MSFT", "Buy", 100),
				trade("t3", 1, "IBM", "Buy", 10)));

		List<Trade> trades = this.tradeService.getTradesByAccountID(1);

		assertEquals(2, trades.size());
		assertTrue(trades.stream().allMatch(trade -> trade.getAccountId() == 1));
		verify(this.tradeRepository).findByAccountId(1);
	}

	@Test
	@DisplayName("getTradesByAccountID returns an empty list for an account without trades")
	void getTradesByAccountIdReturnsEmptyListForUnknownAccount() {
		when(this.tradeRepository.findByAccountId(404)).thenReturn(Collections.emptyList());

		assertTrue(this.tradeService.getTradesByAccountID(404).isEmpty());
	}

	@Test
	@DisplayName("Buy and sell trades net out to the resulting position quantity")
	void buyAndSellTradesNetToPositionQuantity() {
		when(this.tradeRepository.findByAccountId(1)).thenReturn(Arrays.asList(
				trade("t1", 1, "MSFT", "Buy", 100),
				trade("t2", 1, "MSFT", "Sell", 40),
				trade("t3", 1, "AAPL", "Buy", 25)));

		List<Trade> trades = this.tradeService.getTradesByAccountID(1);

		assertEquals(60, netQuantity(trades, "MSFT"));
		assertEquals(25, netQuantity(trades, "AAPL"));
	}

	@Test
	@DisplayName("A newly created trade defaults to the UNSET state")
	void newTradeDefaultsToUnsetState() {
		when(this.tradeRepository.findByAccountId(1)).thenReturn(Collections.singletonList(new Trade()));

		assertEquals("UNSET", this.tradeService.getTradesByAccountID(1).get(0).getState());
	}

	private int netQuantity(List<Trade> trades, String security) {
		return trades.stream()
				.filter(trade -> security.equals(trade.getSecurity()))
				.mapToInt(trade -> "Sell".equalsIgnoreCase(trade.getSide()) ? -trade.getQuantity() : trade.getQuantity())
				.sum();
	}
}
