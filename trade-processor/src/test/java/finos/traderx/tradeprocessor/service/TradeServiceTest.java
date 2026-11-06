package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

class TradeServiceTest {

	private TradeService service;
	private TradeRepository tradeRepository;
	private PositionRepository positionRepository;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		service = new TradeService();
		tradeRepository = mock(TradeRepository.class);
		positionRepository = mock(PositionRepository.class);
		ReflectionTestUtils.setField(service, "tradeRepository", tradeRepository);
		ReflectionTestUtils.setField(service, "positionRepository", positionRepository);
		ReflectionTestUtils.setField(service, "tradePublisher", mock(Publisher.class));
		ReflectionTestUtils.setField(service, "positionPublisher", mock(Publisher.class));
	}

	private static TradeOrder order(TradeSide side, Integer quantity) {
		TradeOrder order = new TradeOrder("o-1", 22214, "MSFT", TradeSide.Buy, 1);
		ReflectionTestUtils.setField(order, "side", side);
		ReflectionTestUtils.setField(order, "quantity", quantity);
		return order;
	}

	private void assertRejected(TradeOrder order) {
		assertThrows(InvalidTradeOrderException.class, () -> service.processTrade(order));
		verifyNoInteractions(tradeRepository);
		verify(positionRepository, never()).save(any());
	}

	@Test
	void rejectsNullQuantity() {
		assertRejected(order(TradeSide.Buy, null));
	}

	@Test
	void rejectsZeroQuantity() {
		assertRejected(order(TradeSide.Buy, 0));
	}

	@Test
	void rejectsNegativeQuantity() {
		assertRejected(order(TradeSide.Buy, -1000000));
	}

	@Test
	void rejectsNullSide() {
		assertRejected(order(null, 10));
	}

	@Test
	void rejectsNullOrder() {
		assertRejected(null);
	}

	@Test
	void rejectsBuyThatOverflowsPosition() {
		Position existing = new Position();
		existing.setAccountId(22214);
		existing.setSecurity("MSFT");
		existing.setQuantity(Integer.MAX_VALUE);
		when(positionRepository.findByAccountIdAndSecurity(22214, "MSFT")).thenReturn(existing);

		assertRejected(order(TradeSide.Buy, 1));
		assertEquals(Integer.MAX_VALUE, existing.getQuantity());
	}

	@Test
	void rejectsSellThatUnderflowsPosition() {
		Position existing = new Position();
		existing.setAccountId(22214);
		existing.setSecurity("MSFT");
		existing.setQuantity(Integer.MIN_VALUE);
		when(positionRepository.findByAccountIdAndSecurity(22214, "MSFT")).thenReturn(existing);

		assertRejected(order(TradeSide.Sell, 1));
	}

	@Test
	void booksValidBuyAndSell() {
		TradeBookingResult buy = service.processTrade(order(TradeSide.Buy, 100));
		assertEquals(100, buy.getPosition().getQuantity());

		when(positionRepository.findByAccountIdAndSecurity(22214, "MSFT")).thenReturn(buy.getPosition());
		TradeBookingResult sell = service.processTrade(order(TradeSide.Sell, 40));
		assertEquals(60, sell.getPosition().getQuantity());
		verify(positionRepository, org.mockito.Mockito.times(2)).save(any());
	}
}
