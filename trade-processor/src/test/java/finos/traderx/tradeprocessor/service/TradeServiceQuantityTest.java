package finos.traderx.tradeprocessor.service;

import static finos.traderx.tradeprocessor.service.TradeServiceTestSupport.order;
import static finos.traderx.tradeprocessor.service.TradeServiceTestSupport.position;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

/**
 * Quantity and position arithmetic corner cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServiceQuantityTest {

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

	private void echoSaves() {
		when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));
		when(positionRepository.save(any(Position.class))).thenAnswer(i -> i.getArgument(0));
	}

	/** QT-01 */
	@Test
	@DisplayName("QT-01 buy then sell of equal size nets a position row of quantity 0")
	void buyThenSellNetsToZero() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);
		TradeBookingResult buy = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 100));
		assertEquals(100, buy.getPosition().getQuantity());

		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(buy.getPosition());
		TradeBookingResult sell = tradeService.processTrade(order(1, "IBM", TradeSide.Sell, 100));

		assertEquals(0, sell.getPosition().getQuantity(), "flat position");
		assertNotNull(sell.getPosition(), "the row is kept, not deleted");
		verify(positionRepository, org.mockito.Mockito.times(2)).save(any(Position.class));
	}

	/** QT-02 */
	@Test
	@DisplayName("QT-02 selling with no existing position creates a short (negative) position, unvalidated")
	void sellWithoutPositionGoesShort() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Sell, 50));

		assertEquals(-50, result.getPosition().getQuantity(),
				"no short-selling validation exists: the position simply goes negative");
	}

	/** QT-03 */
	@Test
	@DisplayName("QT-03 selling more than held oversells into a negative position")
	void oversellGoesNegative() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 10));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Sell, 40));

		assertEquals(-30, result.getPosition().getQuantity(), "no available-quantity check");
	}

	/** QT-04 */
	@Test
	@DisplayName("QT-04 a zero quantity order is booked as a real trade with a zero-delta position update")
	void zeroQuantityIsBooked() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 25));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 0));

		assertEquals(0, result.getTrade().getQuantity(), "zero-quantity trade is accepted");
		assertEquals(25, result.getPosition().getQuantity(), "position unchanged");
		verify(tradeRepository, org.mockito.Mockito.times(2)).save(any(Trade.class));
		verify(positionRepository).save(any(Position.class));
	}

	/** QT-05 */
	@Test
	@DisplayName("QT-05 a negative quantity Buy behaves as a sell (sign is applied on top of the raw quantity)")
	void negativeQuantityBuyBecomesASell() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 100));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, -30));

		assertEquals(70, result.getPosition().getQuantity(),
				"Buy of -30 decreases the position: quantity sign is never validated");
		assertEquals(TradeSide.Buy, result.getTrade().getSide(),
				"the trade is still recorded as a Buy, so the blotter and the position disagree");
	}

	/** QT-06 */
	@Test
	@DisplayName("QT-06 a negative quantity Sell increases the position")
	void negativeQuantitySellIncreasesPosition() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 100));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Sell, -30));

		assertEquals(130, result.getPosition().getQuantity());
	}

	/** QT-07 - LATENT BUG */
	@Test
	@Disabled("LATENT BUG: int overflow in TradeService.processTrade line 60 wraps a huge long position to a negative quantity")
	@DisplayName("QT-07 Integer.MAX_VALUE buy on top of an existing position must not overflow")
	void hugeBuyMustNotOverflow() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 10));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, Integer.MAX_VALUE));

		assertTrue(result.getPosition().getQuantity() > 0,
				"a long position plus a buy must stay positive; it currently wraps to " + result.getPosition().getQuantity());
	}

	/** QT-08 - documents the actual (buggy) overflow behaviour so the suite stays green */
	@Test
	@DisplayName("QT-08 documents the observed overflow: 10 + Integer.MAX_VALUE wraps to a negative position")
	void hugeBuyCurrentlyOverflows() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(position(1, "IBM", 10));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, Integer.MAX_VALUE));

		assertEquals(10 + Integer.MAX_VALUE, result.getPosition().getQuantity());
		assertTrue(result.getPosition().getQuantity() < 0, "observed: silently negative");
	}

	/** QT-09 */
	@Test
	@DisplayName("QT-09 a null quantity throws NullPointerException before anything is persisted")
	void nullQuantityThrowsAndPersistsNothing() throws Exception {
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);

		assertThrows(NullPointerException.class,
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, null)));

		verify(tradeRepository, never()).save(any(Trade.class));
		verify(positionRepository, never()).save(any(Position.class));
		verify(tradePublisher, never()).publish(anyString(), any(Trade.class));
	}

	/** QT-10 */
	@Test
	@DisplayName("QT-10 a null security is booked and creates a position keyed on a null security")
	void nullSecurityIsAccepted() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, null)).thenReturn(null);

		TradeBookingResult result = tradeService.processTrade(order(1, null, TradeSide.Buy, 10));

		assertNull(result.getTrade().getSecurity(), "no ticker validation in the processor");
		assertNull(result.getPosition().getSecurity());
		assertEquals(10, result.getPosition().getQuantity());
	}

	/** QT-11 */
	@Test
	@DisplayName("QT-11 an empty and an over-long security are both accepted by the service layer")
	void emptyAndOverlongSecurityAccepted() {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(any(), anyString())).thenReturn(null);

		TradeBookingResult empty = tradeService.processTrade(order(1, "", TradeSide.Buy, 10));
		assertEquals("", empty.getTrade().getSecurity());

		String longTicker = "X".repeat(200);
		TradeBookingResult overlong = tradeService.processTrade(order(1, longTicker, TradeSide.Buy, 10));
		assertEquals(longTicker, overlong.getTrade().getSecurity(),
				"the service never checks the 50 character SECURITY column limit; the DB write is what fails");
	}

	/** QT-12 */
	@Test
	@DisplayName("QT-12 a null accountId is booked and produces a position with a null account key")
	void nullAccountIdIsAccepted() throws Exception {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(null, "IBM")).thenReturn(null);

		TradeBookingResult result = tradeService.processTrade(order(null, "IBM", TradeSide.Buy, 10));

		assertNull(result.getTrade().getAccountId(), "no account validation in the processor");
		assertNull(result.getPosition().getAccountId());
		verify(tradePublisher).publish("/accounts/null/trades", result.getTrade());
	}
}
