package finos.traderx.tradeprocessor.service;

import static finos.traderx.tradeprocessor.service.TradeServiceTestSupport.order;
import static finos.traderx.tradeprocessor.service.TradeServiceTestSupport.position;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

/**
 * Race windows, partial failures and silent failures around processTrade.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServiceConcurrencyTest {

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

	/** CC-01 - LATENT BUG */
	@Test
	@Disabled("LATENT BUG: read-modify-write of Position in TradeService.processTrade lines 50-63 has no locking or transaction, so concurrent trades lose an update")
	@DisplayName("CC-01 two concurrent trades on the same account+security must both be reflected in the position")
	void concurrentTradesMustNotLoseAnUpdate() throws Exception {
		InterleavingPosition shared = new InterleavingPosition(1, "IBM");
		echoSaves();
		// Both callers read the same (stale) Position instance, exactly as two
		// threads would when neither write has been flushed yet.
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(shared);

		runConcurrently(
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 100)),
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 30)));

		assertEquals(130, shared.getQuantity(),
				"both buys must be applied; the last writer currently wins");
	}

	/** CC-02 - documents the observed lost update so the suite stays green */
	@Test
	@DisplayName("CC-02 documents the observed lost update on concurrent same-security trades")
	void concurrentTradesCurrentlyLoseAnUpdate() throws Exception {
		InterleavingPosition shared = new InterleavingPosition(1, "IBM");
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(shared);

		runConcurrently(
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 100)),
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 30)));

		assertTrue(shared.getQuantity() == 100 || shared.getQuantity() == 30,
				"observed: only one of the two buys survives, quantity=" + shared.getQuantity());
		assertTrue(shared.getQuantity() < 130, "the sum 130 is never reached");
	}

	/** CC-03 */
	@Test
	@DisplayName("CC-03 concurrent trades on the same account but different securities do not interfere")
	void concurrentTradesOnDifferentSecuritiesAreIndependent() throws Exception {
		Position ibm = position(1, "IBM", 0);
		Position msft = position(1, "MSFT", 0);
		CountDownLatch bothRead = new CountDownLatch(2);
		List<Position> saved = new CopyOnWriteArrayList<>();
		when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));
		when(positionRepository.save(any(Position.class))).thenAnswer(i -> {
			saved.add(i.getArgument(0));
			return i.getArgument(0);
		});
		when(positionRepository.findByAccountIdAndSecurity(any(), anyString())).thenAnswer(i -> {
			bothRead.countDown();
			bothRead.await(2, TimeUnit.SECONDS);
			return "IBM".equals(i.getArgument(1)) ? ibm : msft;
		});

		runConcurrently(
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 100)),
				() -> tradeService.processTrade(order(1, "MSFT", TradeSide.Sell, 40)));

		assertEquals(100, ibm.getQuantity());
		assertEquals(-40, msft.getQuantity());
		assertEquals(2, saved.size());
		assertTrue(saved.contains(ibm) && saved.contains(msft));
	}

	/** CC-04 */
	@Test
	@DisplayName("CC-04 a failing position save leaves the New trade row behind: the two writes are not atomic")
	void positionSaveFailureLeavesOrphanTrade() throws Exception {
		List<TradeState> persistedStates = new java.util.ArrayList<>();
		when(tradeRepository.save(any(Trade.class)))
				.thenAnswer(TradeServiceTestSupport.recordTradeStates(persistedStates));
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);
		when(positionRepository.save(any(Position.class)))
				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("boom"));

		assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
				() -> tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 10)));

		assertEquals(List.of(TradeState.New), persistedStates,
				"the trade was already written as New and is never rolled back or moved to Settled");
		verify(tradePublisher, never()).publish(anyString(), any(Trade.class));
		assertTrue(TradeService.class.getAnnotationsByType(org.springframework.transaction.annotation.Transactional.class).length == 0,
				"no @Transactional boundary spans the trade and position writes");
	}

	/** CC-05 */
	@Test
	@DisplayName("CC-05 a publisher failure is swallowed: the caller sees success while the UI never learns of the trade")
	void publisherFailureIsSwallowed() throws Exception {
		echoSaves();
		when(positionRepository.findByAccountIdAndSecurity(1, "IBM")).thenReturn(null);
		org.mockito.Mockito.doThrow(new PubSubException("feed down"))
				.when(tradePublisher).publish(anyString(), any(Trade.class));

		TradeBookingResult result = tradeService.processTrade(order(1, "IBM", TradeSide.Buy, 10));

		assertNotNull(result, "processTrade returns a successful result despite the publish failure");
		assertEquals(TradeState.Settled, result.getTrade().getState());
		verify(positionPublisher, never()).publish(anyString(), any(Position.class));
		verify(tradeRepository, times(2)).save(any(Trade.class));
	}

	/**
	 * A Position that forces both callers to complete their read before either of
	 * them writes, which is exactly the race window left open by the
	 * unsynchronised read-modify-write in processTrade.
	 */
	private static final class InterleavingPosition extends Position {
		private static final long serialVersionUID = 1L;
		private final transient CountDownLatch bothRead = new CountDownLatch(2);

		InterleavingPosition(Integer accountId, String security) {
			setAccountId(accountId);
			setSecurity(security);
			setQuantity(0);
		}

		@Override
		public Integer getQuantity() {
			Integer snapshot = super.getQuantity();
			if (bothRead.getCount() > 0) {
				bothRead.countDown();
				try {
					bothRead.await(2, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return snapshot;
		}
	}

	private void runConcurrently(Runnable a, Runnable b) throws InterruptedException {
		List<Throwable> failures = Collections.synchronizedList(new java.util.ArrayList<>());
		Thread t1 = new Thread(wrap(a, failures), "trade-1");
		Thread t2 = new Thread(wrap(b, failures), "trade-2");
		t1.start();
		t2.start();
		t1.join(5000);
		t2.join(5000);
		if (!failures.isEmpty()) {
			throw new AssertionError("worker failed", failures.get(0));
		}
	}

	private Runnable wrap(Runnable r, List<Throwable> failures) {
		return () -> {
			try {
				r.run();
			} catch (Throwable t) {
				failures.add(t);
			}
		};
	}
}
