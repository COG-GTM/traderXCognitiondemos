package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	@Mock
	TradeRepository tradeRepository;

	@Mock
	PositionRepository positionRepository;

	@Mock
	TradeOrderValidator tradeOrderValidator;

	@Mock
	Publisher<Trade> tradePublisher;

	@Mock
	Publisher<Position> positionPublisher;

	@InjectMocks
	TradeService tradeService;

	@Test
	void rejectedOrderIsNeverPersistedOrPublished() {
		TradeOrder order = new TradeOrder("o1", 22214, "X", TradeSide.Buy, Integer.MIN_VALUE);
		doThrow(new InvalidTradeOrderException("bad")).when(tradeOrderValidator).validate(order);

		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order));

		verifyNoInteractions(tradeRepository, positionRepository, tradePublisher, positionPublisher);
	}

	@Test
	void validOrderUpdatesPosition() throws Exception {
		TradeOrder order = new TradeOrder("o1", 22214, "IBM", TradeSide.Sell, 40);
		Position existing = new Position();
		existing.setAccountId(22214);
		existing.setSecurity("IBM");
		existing.setQuantity(100);
		when(positionRepository.findByAccountIdAndSecurity(22214, "IBM")).thenReturn(existing);

		TradeBookingResult result = tradeService.processTrade(order);

		verify(tradeOrderValidator).validate(order);
		assertEquals(60, result.getPosition().getQuantity());
		verify(positionRepository).save(existing);
		verify(tradePublisher).publish(any(), any());
	}

	@Test
	void orderThatWouldOverflowPositionIsRejected() {
		TradeOrder order = new TradeOrder("o1", 22214, "IBM", TradeSide.Buy, 10);
		Position existing = new Position();
		existing.setAccountId(22214);
		existing.setSecurity("IBM");
		existing.setQuantity(Integer.MAX_VALUE - 5);
		when(positionRepository.findByAccountIdAndSecurity(22214, "IBM")).thenReturn(existing);

		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order));

		verify(positionRepository, never()).save(any());
		verify(tradeRepository, never()).save(any());
	}
}
