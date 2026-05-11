package finos.traderx.tradeprocessor.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import finos.traderx.tradeprocessor.model.*;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;
import finos.traderx.tradeprocessor.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that boots the full Spring context (with in-memory H2)
 * and verifies that processing a trade produces the required audit records.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit-test",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "trade.feed.address=http://localhost:19999"
})
class TradeAuditIntegrationTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }

    @Test
    void processTrade_producesAuditRecordForEveryTradeProcessed() {
        TradeOrder order = new TradeOrder("integration-user", 1, "AAPL", TradeSide.Buy, 100);

        tradeService.processTrade(order);

        List<ILoggingEvent> auditEvents = listAppender.list;

        // Exactly 3 audit events: TRADE_RECEIVED, TRADE_STATE_CHANGE, TRADE_SETTLED
        assertEquals(3, auditEvents.size(),
                "Every trade processed must produce exactly 3 audit records");

        String receivedMsg = auditEvents.get(0).getFormattedMessage();
        String stateChangeMsg = auditEvents.get(1).getFormattedMessage();
        String settledMsg = auditEvents.get(2).getFormattedMessage();

        // Verify TRADE_RECEIVED
        assertTrue(receivedMsg.contains("TRADE_RECEIVED"));
        assertTrue(receivedMsg.contains("AAPL"));
        assertTrue(receivedMsg.contains("100"));
        assertTrue(receivedMsg.contains("Buy"));

        // Verify TRADE_STATE_CHANGE (New -> Processing)
        assertTrue(stateChangeMsg.contains("TRADE_STATE_CHANGE"));
        assertTrue(stateChangeMsg.contains("Processing"));

        // Verify TRADE_SETTLED
        assertTrue(settledMsg.contains("TRADE_SETTLED"));
        assertTrue(settledMsg.contains("Settled"));

        // All audit events must contain the required fields per data-governance standard
        for (ILoggingEvent event : auditEvents) {
            String msg = event.getFormattedMessage();
            assertTrue(msg.contains("timestamp"), "Audit record must contain timestamp");
            assertTrue(msg.contains("tradeId"), "Audit record must contain tradeId");
            assertTrue(msg.contains("accountId"), "Audit record must contain accountId");
            assertTrue(msg.contains("security"), "Audit record must contain security");
            assertTrue(msg.contains("quantity"), "Audit record must contain quantity");
            assertTrue(msg.contains("side"), "Audit record must contain side");
            assertTrue(msg.contains("initiator"), "Audit record must contain initiator");
        }

        // Verify the trade was actually persisted (end-to-end)
        List<Trade> trades = tradeRepository.findByAccountId(1);
        assertFalse(trades.isEmpty(), "Trade should be persisted in the database");
        assertEquals(TradeState.Settled, trades.get(0).getState());
    }

    @Test
    void processTrade_multipleTradesEachProduceAuditRecords() {
        TradeOrder order1 = new TradeOrder("user-a", 2, "MSFT", TradeSide.Buy, 50);
        TradeOrder order2 = new TradeOrder("user-b", 2, "MSFT", TradeSide.Sell, 25);

        tradeService.processTrade(order1);
        tradeService.processTrade(order2);

        List<ILoggingEvent> auditEvents = listAppender.list;

        // 3 audit events per trade x 2 trades = 6 total
        assertEquals(6, auditEvents.size(),
                "Each trade processed must produce its own set of audit records");

        // First trade events
        assertTrue(auditEvents.get(0).getFormattedMessage().contains("TRADE_RECEIVED"));
        assertTrue(auditEvents.get(0).getFormattedMessage().contains("Buy"));

        // Second trade events
        assertTrue(auditEvents.get(3).getFormattedMessage().contains("TRADE_RECEIVED"));
        assertTrue(auditEvents.get(3).getFormattedMessage().contains("Sell"));
    }
}
