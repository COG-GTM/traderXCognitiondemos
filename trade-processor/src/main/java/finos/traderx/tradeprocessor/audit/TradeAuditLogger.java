package finos.traderx.tradeprocessor.audit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeState;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Dedicated audit logger for trade-processing events.
 * Writes to the "AUDIT" logger (configured with a separate appender in logback-spring.xml)
 * using structured key-value pairs that serialize as JSON for SIEM ingestion.
 */
@Component
public class TradeAuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void logTradeReceived(TradeOrder order, String tradeId) {
        AuditEvent event = AuditEvent.builder()
                .eventType("TRADE_RECEIVED")
                .timestamp(Instant.now())
                .tradeId(tradeId)
                .accountId(order.getAccountId())
                .security(order.getSecurity())
                .quantity(order.getQuantity())
                .side(order.getSide())
                .currentState(TradeState.New)
                .initiator(resolveInitiator(order))
                .build();

        writeAuditLog(event);
    }

    public void logTradeStateChange(Trade trade, TradeState previousState) {
        AuditEvent event = AuditEvent.builder()
                .eventType("TRADE_STATE_CHANGE")
                .timestamp(Instant.now())
                .tradeId(trade.getId())
                .accountId(trade.getAccountId())
                .security(trade.getSecurity())
                .quantity(trade.getQuantity())
                .side(trade.getSide())
                .currentState(trade.getState())
                .previousState(previousState)
                .initiator("SYSTEM")
                .build();

        writeAuditLog(event);
    }

    public void logTradeSettled(Trade trade) {
        AuditEvent event = AuditEvent.builder()
                .eventType("TRADE_SETTLED")
                .timestamp(Instant.now())
                .tradeId(trade.getId())
                .accountId(trade.getAccountId())
                .security(trade.getSecurity())
                .quantity(trade.getQuantity())
                .side(trade.getSide())
                .currentState(trade.getState())
                .previousState(TradeState.Processing)
                .initiator("SYSTEM")
                .build();

        writeAuditLog(event);
    }

    private void writeAuditLog(AuditEvent event) {
        AUDIT.info("{} {} {} {} {} {} {} {} {} {}",
                kv("eventType", event.getEventType()),
                kv("timestamp", event.getTimestamp().toString()),
                kv("tradeId", event.getTradeId()),
                kv("accountId", event.getAccountId()),
                kv("security", event.getSecurity()),
                kv("quantity", event.getQuantity()),
                kv("side", event.getSide()),
                kv("currentState", event.getCurrentState()),
                kv("previousState", event.getPreviousState()),
                kv("initiator", event.getInitiator()));
    }

    private String resolveInitiator(TradeOrder order) {
        if (order.getId() != null && !order.getId().isBlank()) {
            return order.getId();
        }
        return "SYSTEM";
    }
}
