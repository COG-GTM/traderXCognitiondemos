package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.exception.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@DataJpaTest
@Import(TradeService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:tradeservicetest;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"trade.max-quantity=1000"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TradeServiceTest {

	private static final int ACCOUNT = 22214;
	private static final String SECURITY = "IBM";

	@Autowired
	TradeService tradeService;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@MockBean
	Publisher<Trade> tradePublisher;

	@MockBean
	Publisher<Position> positionPublisher;

	@BeforeEach
	void clean() {
		tradeRepository.deleteAll();
		positionRepository.deleteAll();
	}

	@Test
	void buyAndSellUpdatePosition() {
		tradeService.processTrade(order(TradeSide.Buy, 100));
		tradeService.processTrade(order(TradeSide.Sell, 30));
		tradeService.processTrade(order(TradeSide.Sell, 100));

		assertEquals(-30, positionQuantity());
		assertEquals(3, tradeRepository.count());
	}

	@Test
	void acceptsMaxQuantity() {
		tradeService.processTrade(order(TradeSide.Buy, 1000));

		assertEquals(1000, positionQuantity());
	}

	@Test
	void rejectsNonPositiveNullOrOversizedQuantity() {
		for (Integer quantity : new Integer[] { null, 0, -1000, Integer.MIN_VALUE, 1001, Integer.MAX_VALUE }) {
			TradeOrder order = new TradeOrder();
			setField(order, "accountId", ACCOUNT);
			setField(order, "security", SECURITY);
			setField(order, "side", TradeSide.Buy);
			setField(order, "quantity", quantity);
			assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order), "quantity " + quantity);
		}
		assertNothingBooked();
	}

	@Test
	void rejectsMissingFields() {
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(null));
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(new TradeOrder("1", ACCOUNT, " ", TradeSide.Buy, 10)));
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(new TradeOrder("1", ACCOUNT, SECURITY, null, 10)));
		TradeOrder noAccount = new TradeOrder("1", ACCOUNT, SECURITY, TradeSide.Buy, 10);
		setField(noAccount, "accountId", null);
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(noAccount));
		assertNothingBooked();
	}

	@Test
	void rejectsPositionOverflowWithoutPersistingTrade() {
		savePosition(Integer.MAX_VALUE - 5);
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order(TradeSide.Buy, 10)));
		assertEquals(Integer.MAX_VALUE - 5, positionQuantity());

		savePosition(Integer.MIN_VALUE + 5);
		assertThrows(InvalidTradeOrderException.class, () -> tradeService.processTrade(order(TradeSide.Sell, 10)));
		assertEquals(Integer.MIN_VALUE + 5, positionQuantity());

		assertEquals(0, tradeRepository.count());
	}

	@Test
	void concurrentOrdersDoNotLoseUpdates() throws Exception {
		int threads = 32;
		int ordersPerThread = 10;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				tasks.add(() -> {
					start.await();
					for (int j = 0; j < ordersPerThread; j++) {
						tradeService.processTrade(order(TradeSide.Buy, 10));
					}
					return null;
				});
			}
			List<Future<Void>> futures = new ArrayList<>();
			for (Callable<Void> task : tasks) {
				futures.add(executor.submit(task));
			}
			start.countDown();
			for (Future<Void> f : futures) {
				f.get();
			}
		} finally {
			executor.shutdownNow();
		}

		assertEquals(threads * ordersPerThread * 10, positionQuantity());
		assertEquals(threads * ordersPerThread, tradeRepository.count());
	}

	private void assertNothingBooked() {
		assertEquals(0, tradeRepository.count());
		assertEquals(0, positionRepository.count());
		try {
			verify(tradePublisher, never()).publish(anyString(), any());
			verify(positionPublisher, never()).publish(anyString(), any());
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private int positionQuantity() {
		return positionRepository.findByAccountIdAndSecurity(ACCOUNT, SECURITY).getQuantity();
	}

	private void savePosition(int quantity) {
		Position p = new Position();
		p.setAccountId(ACCOUNT);
		p.setSecurity(SECURITY);
		p.setQuantity(quantity);
		positionRepository.save(p);
	}

	private static TradeOrder order(TradeSide side, int quantity) {
		return new TradeOrder(null, ACCOUNT, SECURITY, side, quantity);
	}

	private static void setField(TradeOrder order, String name, Object value) {
		org.springframework.test.util.ReflectionTestUtils.setField(order, name, value);
	}
}
