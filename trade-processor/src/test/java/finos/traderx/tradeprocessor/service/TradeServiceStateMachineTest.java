package finos.traderx.tradeprocessor.service;

import static finos.traderx.tradeprocessor.service.TradeServiceTestSupport.order;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import finos.traderx.tradeprocessor.model.TradeState;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;
import finos.traderx.tradeprocessor.service.TradeService;

/**
 * Lifecycle / state machine behaviour of TradeService.processTrade.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServiceStateMachineTest {

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

	/** LC-01 */
	@Test
	@DisplayName("LC-01 trade is persisted as New then Settled; Processing is never persisted")
	void processingStateIsNeverPersisted() {
		List<TradeState> persistedStates = new ArrayList<>();
		when(tradeRepository.save(any(Trade.class)))
				.thenAnswer(TradeServiceTestSupport.recordTradeStates(persistedStates));
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 10));

		assertEquals(List.of(TradeState.New, TradeState.Settled), persistedStates,
				"processTrade should only ever hand New and Settled trades to the repository");
		assertFalse(persistedStates.contains(TradeState.Processing),
				"the Processing state is set and overwritten between saves, so it is unobservable");
		assertEquals(TradeState.Settled, result.getTrade().getState());
	}

	/** LC-02 */
	@Test
	@DisplayName("LC-02 published trade is the Settled trade, published after both writes")
	void publishedTradeIsSettled() throws Exception {
		when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));
		when(positionRepository.save(any(Position.class))).thenAnswer(i -> i.getArgument(0));
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);

		tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 10));

		ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
		org.mockito.Mockito.verify(tradePublisher).publish(anyString(), captor.capture());
		assertEquals(TradeState.Settled, captor.getValue().getState());
	}

	/** LC-03 */
	@Test
	@DisplayName("LC-03 entity accepts the illegal direct transition New -> Settled with no guard")
	void entityAcceptsNewToSettledDirectly() {
		Trade t = new Trade();
		assertEquals(TradeState.New, t.getState(), "default state");
		t.setState(TradeState.Settled);
		assertEquals(TradeState.Settled, t.getState(),
				"there is no legal-transition validation: New -> Settled is silently accepted");
	}

	/** LC-04 */
	@Test
	@DisplayName("LC-04 entity accepts the illegal backwards transitions Settled -> New and Cancelled -> Settled")
	void entityAcceptsBackwardsTransitions() {
		Trade t = new Trade();
		t.setState(TradeState.Settled);
		t.setState(TradeState.New);
		assertEquals(TradeState.New, t.getState(), "Settled -> New is accepted (no guard)");

		t.setState(TradeState.Cancelled);
		t.setState(TradeState.Settled);
		assertEquals(TradeState.Settled, t.getState(), "Cancelled -> Settled is accepted (no guard)");
	}

	/** LC-05 */
	@Test
	@DisplayName("LC-05 Cancelled is a dead state: no code path produces it and no cancel API exists")
	void cancelledIsADeadState() {
		List<TradeState> persistedStates = new ArrayList<>();
		when(tradeRepository.save(any(Trade.class)))
				.thenAnswer(TradeServiceTestSupport.recordTradeStates(persistedStates));
		when(positionRepository.findByAccountIdAndSecurity(any(), anyString())).thenReturn(null);

		tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 10));
		tradeService.processTrade(order(1, "IBM", TradeSide.Sell, 10));

		assertFalse(persistedStates.contains(TradeState.Cancelled),
				"no processing path ever produces Cancelled");

		boolean hasCancelEntryPoint = false;
		for (Method m : TradeService.class.getDeclaredMethods()) {
			if (m.getName().toLowerCase().contains("cancel")) {
				hasCancelEntryPoint = true;
			}
		}
		assertFalse(hasCancelEntryPoint, "TradeService exposes no way to cancel a trade");
		assertTrue(List.of(TradeState.values()).contains(TradeState.Cancelled),
				"Cancelled is nonetheless declared in the enum - dead state");
	}

	/** LC-06 */
	@Test
	@DisplayName("LC-06 every TradeState fits the STATE column (length 20) and every TradeSide the SIDE column (length 4)")
	void enumValuesFitTheirColumns() throws Exception {
		int stateLength = Trade.class.getDeclaredField("state")
				.getAnnotation(jakarta.persistence.Column.class).length();
		int sideLength = Trade.class.getDeclaredField("side")
				.getAnnotation(jakarta.persistence.Column.class).length();

		assertEquals(20, stateLength);
		assertEquals(4, sideLength);

		for (TradeState s : TradeState.values()) {
			assertTrue(s.name().length() <= stateLength, s + " does not fit STATE(" + stateLength + ")");
		}
		for (TradeSide s : TradeSide.values()) {
			assertTrue(s.name().length() <= sideLength, s + " does not fit SIDE(" + sideLength + ")");
		}
		// Fragility: "Sell" is exactly 4 characters, so any new side longer than
		// four characters (e.g. "Short") would be silently truncated / rejected.
		assertEquals(sideLength, "Sell".length(), "SIDE column has zero headroom");
	}
}
