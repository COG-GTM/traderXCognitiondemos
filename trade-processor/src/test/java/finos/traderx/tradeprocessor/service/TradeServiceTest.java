package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Mock
	TradeRepository tradeRepository;

	@Mock
	PositionRepository positionRepository;

	@Mock
	Publisher<Trade> tradePublisher;

	@Mock
	Publisher<Position> positionPublisher;

	@InjectMocks
	TradeService tradeService;

	private static TradeOrder order(String json) throws Exception {
		return MAPPER.readValue(json, TradeOrder.class);
	}

	private static Position position(int quantity) {
		Position position = new Position();
		position.setAccountId(22);
		position.setSecurity("MSFT");
		position.setQuantity(quantity);
		return position;
	}

	private void assertRejectedAndNothingSaved(TradeOrder order) throws Exception {
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order));
		verify(tradeRepository, never()).save(any());
		verify(positionRepository, never()).save(any());
		verify(tradePublisher, never()).publish(anyString(), any());
		verify(positionPublisher, never()).publish(anyString(), any());
	}

	@Test
	void rejectsNegativeQuantityOnBuy() throws Exception {
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":-2147483000}"));
	}

	@Test
	void rejectsMinusOneQuantity() throws Exception {
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Sell\",\"quantity\":-1}"));
	}

	@Test
	void rejectsZeroQuantity() throws Exception {
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":0}"));
	}

	@Test
	void rejectsNullQuantity() throws Exception {
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\"}"));
	}

	@Test
	void rejectsNullSideInsteadOfBookingSell() throws Exception {
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"quantity\":100}"));
	}

	@Test
	void rejectsMissingAccountOrSecurity() throws Exception {
		assertRejectedAndNothingSaved(order("{\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":100}"));
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"side\":\"Buy\",\"quantity\":100}"));
		assertRejectedAndNothingSaved(null);
	}

	@Test
	void rejectsBuyThatOverflowsExistingPosition() throws Exception {
		when(positionRepository.findByAccountIdAndSecurity(22, "MSFT")).thenReturn(position(500));
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":2147483647}"));
	}

	@Test
	void rejectsSellThatUnderflowsExistingPosition() throws Exception {
		when(positionRepository.findByAccountIdAndSecurity(22, "MSFT")).thenReturn(position(-10));
		assertRejectedAndNothingSaved(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Sell\",\"quantity\":2147483647}"));
	}

	@Test
	void booksValidBuyAndSell() throws Exception {
		Position existing = position(500);
		when(positionRepository.findByAccountIdAndSecurity(22, "MSFT")).thenReturn(existing);

		TradeBookingResult buy = tradeService.processTrade(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":100}"));
		assertEquals(600, buy.getPosition().getQuantity());

		TradeBookingResult sell = tradeService.processTrade(order("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Sell\",\"quantity\":250}"));
		assertEquals(350, sell.getPosition().getQuantity());
		verify(positionRepository, org.mockito.Mockito.times(2)).save(existing);
	}

	@Test
	void createsNewPositionForFirstTrade() throws Exception {
		TradeBookingResult result = tradeService.processTrade(order("{\"accountId\":22,\"security\":\"IBM\",\"side\":\"Sell\",\"quantity\":40}"));
		assertEquals(-40, result.getPosition().getQuantity());
		assertEquals("IBM", result.getPosition().getSecurity());
	}
}
