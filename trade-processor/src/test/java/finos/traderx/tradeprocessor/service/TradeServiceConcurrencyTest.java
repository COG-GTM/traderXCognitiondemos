package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.messaging.Publisher;
import finos.traderx.messaging.Subscriber;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:tradeprocessor;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"trade.feed.address=http://localhost:1",
		"trade.max-order-quantity=1000000"
})
@AutoConfigureMockMvc
class TradeServiceConcurrencyTest {

	private static final int ACCOUNT = 22214;

	@MockBean(name = "tradePublisher")
	Publisher<Trade> tradePublisher;

	@MockBean(name = "positionPublisher")
	Publisher<Position> positionPublisher;

	@MockBean(name = "tradeFeedHandler")
	Subscriber<TradeOrder> tradeFeedHandler;

	@Autowired
	TradeService tradeService;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@Autowired
	MockMvc mockMvc;

	@BeforeEach
	void clean() {
		tradeRepository.deleteAll();
		positionRepository.deleteAll();
	}

	@Test
	void concurrentBuySellPairsOnNewPositionNetToZero() throws Exception {
		runConcurrentBuySellPairs("IBM", 100);

		assertEquals(0, quantity("IBM"));
		assertEquals(200, tradeRepository.findByAccountId(ACCOUNT).size());
	}

	@Test
	void concurrentBuySellPairsOnExistingPositionReturnToStart() throws Exception {
		seedPosition("MS", 1000);

		runConcurrentBuySellPairs("MS", 100);

		assertEquals(1000, quantity("MS"));
		assertEquals(200, tradeRepository.findByAccountId(ACCOUNT).size());
	}

	@Test
	void duplicateDeliveriesOfSameOrderAreBookedOnce() throws Exception {
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			tasks.add(() -> tradeService.processTrade(new TradeOrder("ORDER-1", ACCOUNT, "C", TradeSide.Buy, 5000)));
		}
		runAll(tasks);

		assertEquals(5000, quantity("C"));
		assertEquals(1, tradeRepository.findByAccountId(ACCOUNT).size());
		assertEquals(5000, tradeRepository.findById("ORDER-1").orElseThrow().getQuantity());
		verify(tradePublisher, times(1)).publish(anyString(), any());
	}

	@Test
	void reusingAnOrderIdForADifferentOrderIsRejected() {
		tradeService.processTrade(new TradeOrder("ORDER-2", ACCOUNT, "BAC", TradeSide.Buy, 100));

		assertThrows(DuplicateTradeIdException.class,
				() -> tradeService.processTrade(new TradeOrder("ORDER-2", 52355, "BAC", TradeSide.Buy, 100)));

		assertEquals(100, quantity("BAC"));
		assertNull(positionRepository.findByAccountIdAndSecurity(52355, "BAC"));
	}

	@Test
	void positionOverflowIsRejectedAndNothingIsBooked() {
		seedPosition("AAPL", Integer.MAX_VALUE - 10);

		assertThrows(InvalidTradeOrderException.class,
				() -> tradeService.processTrade(new TradeOrder("ORDER-3", ACCOUNT, "AAPL", TradeSide.Buy, 1000)));

		assertEquals(Integer.MAX_VALUE - 10, quantity("AAPL"));
		assertTrue(tradeRepository.findById("ORDER-3").isEmpty());
	}

	@Test
	void quantitiesOutsideTheOrderLimitAreRejected() {
		assertThrows(InvalidTradeOrderException.class,
				() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, "IBM", TradeSide.Buy, Integer.MAX_VALUE)));
		assertThrows(InvalidTradeOrderException.class,
				() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, "IBM", TradeSide.Buy, 1_000_001)));
		assertThrows(InvalidTradeOrderException.class,
				() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, "IBM", TradeSide.Sell, 0)));
		assertThrows(InvalidTradeOrderException.class,
				() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, "IBM", TradeSide.Sell, -5)));

		assertNull(positionRepository.findByAccountIdAndSecurity(ACCOUNT, "IBM"));
		assertTrue(tradeRepository.findAll().isEmpty());
	}

	@Test
	void orderEndpointMapsRejectionsToClientErrors() throws Exception {
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"ORDER-4\",\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":2147483647}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"ORDER-5\",\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Buy\",\"quantity\":10}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/tradeservice/order").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"ORDER-5\",\"accountId\":22214,\"security\":\"IBM\",\"side\":\"Sell\",\"quantity\":10}"))
				.andExpect(status().isConflict());

		assertEquals(10, quantity("IBM"));
	}

	private void runConcurrentBuySellPairs(String security, int pairs) throws Exception {
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int i = 0; i < pairs; i++) {
			tasks.add(() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, security, TradeSide.Buy, 1)));
			tasks.add(() -> tradeService.processTrade(new TradeOrder(null, ACCOUNT, security, TradeSide.Sell, 1)));
		}
		runAll(tasks);
	}

	private static void runAll(List<Callable<Object>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<Object>> futures = new ArrayList<>();
			for (Callable<Object> task : tasks) {
				futures.add(executor.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			for (Future<Object> future : futures) {
				future.get();
			}
		}
	}

	private void seedPosition(String security, int quantity) {
		Position position = new Position();
		position.setAccountId(ACCOUNT);
		position.setSecurity(security);
		position.setQuantity(quantity);
		position.setUpdated(new Date());
		positionRepository.save(position);
	}

	private int quantity(String security) {
		return positionRepository.findByAccountIdAndSecurity(ACCOUNT, security).getQuantity();
	}
}
