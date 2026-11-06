package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.messaging.Subscriber;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.PositionID;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:tradeprocessor;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.datasource.hikari.maximum-pool-size=20"
})
class TradeServiceConcurrencyTest {

	private static final int ACCOUNT = 22;
	private static final String SECURITY = "MSFT";
	private static final int ORDERS = 100;

	@Autowired
	TradeService tradeService;

	@Autowired
	PositionRepository positionRepository;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PlatformTransactionManager transactionManager;

	@MockBean(name = "tradePublisher")
	Publisher<Trade> tradePublisher;

	@MockBean(name = "positionPublisher")
	Publisher<Position> positionPublisher;

	@MockBean(name = "tradeFeedHandler")
	Subscriber<TradeOrder> tradeFeedHandler;

	@BeforeEach
	void clean() {
		tradeRepository.deleteAll();
		positionRepository.deleteAll();
	}

	@Test
	void concurrentBuysOnExistingPositionAreAllApplied() throws Exception {
		Position seed = new Position();
		seed.setAccountId(ACCOUNT);
		seed.setSecurity(SECURITY);
		seed.setQuantity(0);
		positionRepository.save(seed);

		runConcurrentBuys(true);

		assertEquals(ORDERS, positionRepository.findById(position()).orElseThrow().getQuantity());
		assertEquals(ORDERS, tradeRepository.count());
	}

	@Test
	void concurrentBuysOnNewPositionNeverLoseOrOrphanUpdates() throws Exception {
		int booked = runConcurrentBuys(false);

		// Racing inserts of the first row for a key may be rejected, but every booked trade must be in the position.
		assertTrue(booked > 0);
		assertEquals(booked, positionRepository.findById(position()).orElseThrow().getQuantity());
		assertEquals(booked, tradeRepository.count());
	}

	@Test
	void rolledBackBookingIsNotPublished() throws Exception {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			tradeService.processTrade(new TradeOrder(null, ACCOUNT, SECURITY, TradeSide.Buy, 1));
			status.setRollbackOnly();
		});

		verify(tradePublisher, never()).publish(anyString(), any());
		verify(positionPublisher, never()).publish(anyString(), any());
		assertEquals(0, tradeRepository.count());
	}

	@Test
	void committedBookingIsPublished() throws Exception {
		tradeService.processTrade(new TradeOrder(null, ACCOUNT, SECURITY, TradeSide.Buy, 1));

		verify(tradePublisher).publish(anyString(), any());
		verify(positionPublisher).publish(anyString(), any());
	}

	private int runConcurrentBuys(boolean failOnError) throws Exception {
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int i = 0; i < ORDERS; i++) {
			tasks.add(() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, SECURITY, TradeSide.Buy, 1)));
		}
		ExecutorService pool = Executors.newFixedThreadPool(20);
		int booked = 0;
		try {
			for (Future<Object> f : pool.invokeAll(tasks)) {
				try {
					f.get();
					booked++;
				} catch (ExecutionException e) {
					if (failOnError || !(e.getCause() instanceof DataIntegrityViolationException)) {
						throw e;
					}
				}
			}
		} finally {
			pool.shutdown();
		}
		return booked;
	}

	private PositionID position() {
		return new PositionID(ACCOUNT, SECURITY);
	}
}
