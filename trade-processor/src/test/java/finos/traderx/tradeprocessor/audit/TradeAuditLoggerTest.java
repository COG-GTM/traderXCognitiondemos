package finos.traderx.tradeprocessor.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeAuditLoggerTest {

    private TradeAuditLogger auditLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        auditLogger = new TradeAuditLogger();

        Logger auditLogbackLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogbackLogger.addAppender(listAppender);
    }

    @Test
    void logTradeReceived_emitsAuditEvent() {
        TradeOrder order = new TradeOrder("user-123", 42, "AAPL", TradeSide.Buy, 100);
        String tradeId = "trade-abc-123";

        auditLogger.logTradeReceived(order, tradeId);

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size());

        ILoggingEvent event = events.get(0);
        assertEquals("INFO", event.getLevel().toString());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("TRADE_RECEIVED"), "Should contain event type");
        assertTrue(message.contains("trade-abc-123"), "Should contain trade ID");
        assertTrue(message.contains("42"), "Should contain account ID");
        assertTrue(message.contains("AAPL"), "Should contain security");
        assertTrue(message.contains("100"), "Should contain quantity");
        assertTrue(message.contains("Buy"), "Should contain side");
    }

    @Test
    void logTradeReceived_usesOrderIdAsInitiator() {
        TradeOrder order = new TradeOrder("trader-jane", 1, "MSFT", TradeSide.Sell, 50);

        auditLogger.logTradeReceived(order, "trade-xyz");

        ILoggingEvent event = listAppender.list.get(0);
        assertTrue(event.getFormattedMessage().contains("trader-jane"),
                "Should use order ID as initiator when present");
    }

    @Test
    void logTradeReceived_defaultsToSystemInitiator() {
        TradeOrder order = new TradeOrder(null, 1, "MSFT", TradeSide.Sell, 50);

        auditLogger.logTradeReceived(order, "trade-xyz");

        ILoggingEvent event = listAppender.list.get(0);
        assertTrue(event.getFormattedMessage().contains("SYSTEM"),
                "Should default to SYSTEM when order ID is null");
    }

    @Test
    void logTradeStateChange_emitsAuditEventWithPreviousState() {
        Trade trade = new Trade();
        trade.setId("trade-456");
        trade.setAccountId(7);
        trade.setSecurity("GOOG");
        trade.setQuantity(200);
        trade.setSide(TradeSide.Buy);
        trade.setState(TradeState.Processing);

        auditLogger.logTradeStateChange(trade, TradeState.New);

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size());

        String message = events.get(0).getFormattedMessage();
        assertTrue(message.contains("TRADE_STATE_CHANGE"), "Should contain event type");
        assertTrue(message.contains("trade-456"), "Should contain trade ID");
        assertTrue(message.contains("Processing"), "Should contain current state");
        assertTrue(message.contains("New"), "Should contain previous state");
    }

    @Test
    void logTradeSettled_emitsAuditEvent() {
        Trade trade = new Trade();
        trade.setId("trade-789");
        trade.setAccountId(99);
        trade.setSecurity("TSLA");
        trade.setQuantity(10);
        trade.setSide(TradeSide.Sell);
        trade.setState(TradeState.Settled);

        auditLogger.logTradeSettled(trade);

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size());

        String message = events.get(0).getFormattedMessage();
        assertTrue(message.contains("TRADE_SETTLED"), "Should contain event type");
        assertTrue(message.contains("trade-789"), "Should contain trade ID");
        assertTrue(message.contains("Settled"), "Should contain current state");
        assertTrue(message.contains("Processing"), "Should contain previous state (Processing)");
    }

    @Test
    void allRequiredFieldsPresent_inEveryAuditEvent() {
        TradeOrder order = new TradeOrder("user-1", 10, "IBM", TradeSide.Buy, 500);

        auditLogger.logTradeReceived(order, "trade-full-check");

        String message = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("eventType"), "Must include eventType field");
        assertTrue(message.contains("timestamp"), "Must include timestamp field");
        assertTrue(message.contains("tradeId"), "Must include tradeId field");
        assertTrue(message.contains("accountId"), "Must include accountId field");
        assertTrue(message.contains("security"), "Must include security field");
        assertTrue(message.contains("quantity"), "Must include quantity field");
        assertTrue(message.contains("side"), "Must include side field");
        assertTrue(message.contains("currentState"), "Must include currentState field");
        assertTrue(message.contains("initiator"), "Must include initiator field");
    }
}
