package finos.traderx.tradeprocessor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import finos.traderx.messaging.Envelope;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.service.TradeService;

/**
 * Delivery semantics of the socket.io trade feed consumer.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeFeedHandlerTest {

	@Mock
	TradeService tradeService;

	@Mock
	Envelope<?> envelope;

	private TradeFeedHandler handler;

	@BeforeEach
	void setUp() throws Exception {
		handler = new TradeFeedHandler();
		Field f = TradeFeedHandler.class.getDeclaredField("tradeService");
		f.setAccessible(true);
		f.set(handler, tradeService);
	}

	/** FD-01 */
	@Test
	@DisplayName("FD-01 a poison message is swallowed and acknowledged: no rethrow, no retry, no DLQ")
	void poisonMessageIsSwallowed() {
		when(tradeService.processTrade(any(TradeOrder.class)))
				.thenThrow(new IllegalStateException("poison"));

		TradeOrder order = new TradeOrder("order-1", 1, "IBM", TradeSide.Buy, 10);
		assertDoesNotThrow(() -> handler.onMessage(envelope, order));

		verify(tradeService, times(1)).processTrade(order);
		// The handler has no redelivery or dead-letter collaborator at all.
		for (Field field : TradeFeedHandler.class.getDeclaredFields()) {
			String name = field.getName().toLowerCase();
			assertTrue(!name.contains("retry") && !name.contains("dead") && !name.contains("dlq"),
					"unexpected retry/DLQ collaborator: " + field.getName());
		}
	}

	/** FD-02 */
	@Test
	@DisplayName("FD-02 a NullPointerException from an incomplete order is also swallowed")
	void nullPointerFromIncompleteOrderIsSwallowed() {
		when(tradeService.processTrade(any(TradeOrder.class))).thenThrow(new NullPointerException());

		assertDoesNotThrow(() -> handler.onMessage(envelope, new TradeOrder()));
		verify(tradeService).processTrade(any(TradeOrder.class));
	}

	/** FD-03 */
	@Test
	@DisplayName("FD-03 duplicate delivery of the same order books the trade twice: no idempotency on order id")
	void duplicateDeliveryDoubleBooks() {
		List<Trade> booked = new ArrayList<>();
		when(tradeService.processTrade(any(TradeOrder.class))).thenAnswer(i -> {
			TradeOrder o = i.getArgument(0);
			Trade t = new Trade();
			t.setId(UUID.randomUUID().toString()); // mirrors TradeService: server-side id
			t.setAccountId(o.getAccountId());
			t.setSecurity(o.getSecurity());
			t.setQuantity(o.getQuantity());
			booked.add(t);
			return new TradeBookingResult(t, new Position());
		});

		TradeOrder order = new TradeOrder("order-1", 1, "IBM", TradeSide.Buy, 10);
		handler.onMessage(envelope, order);
		handler.onMessage(envelope, order); // at-least-once redelivery

		verify(tradeService, times(2)).processTrade(order);
		assertEquals(2, booked.size(), "the same order is booked twice");
		assertNotEquals(booked.get(0).getId(), booked.get(1).getId(),
				"trade ids are generated server side, so the duplicate is indistinguishable");
		assertEquals("order-1", order.getId(), "the order carries an id that is never used for de-duplication");
	}
}
