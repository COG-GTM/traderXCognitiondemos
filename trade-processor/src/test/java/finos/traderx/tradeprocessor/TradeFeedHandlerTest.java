package finos.traderx.tradeprocessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import finos.traderx.messaging.Envelope;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.service.TradeService;

class TradeFeedHandlerTest {

	private final TradeService tradeService = mock(TradeService.class);
	private final TradeOrder order = new TradeOrder("o-1", 22214, "MSFT", TradeSide.Buy, 10);

	private TradeFeedHandler handler() {
		TradeFeedHandler handler = new TradeFeedHandler();
		ReflectionTestUtils.setField(handler, "tradeService", tradeService);
		return handler;
	}

	@SuppressWarnings("unchecked")
	private static Envelope<?> from(String sender) {
		Envelope<Object> envelope = mock(Envelope.class);
		when(envelope.getFrom()).thenReturn(sender);
		return envelope;
	}

	@Test
	void ignoresOrdersNotStampedByTradeService() {
		handler().onMessage(from("anonymous-viewer"), order);
		handler().onMessage(from(null), order);
		verify(tradeService, never()).processTrade(order);
	}

	@Test
	void processesOrdersFromTradeService() {
		handler().onMessage(from("trade-service"), order);
		verify(tradeService).processTrade(order);
	}
}
