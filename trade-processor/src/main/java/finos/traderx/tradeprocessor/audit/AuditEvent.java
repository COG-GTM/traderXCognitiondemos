package finos.traderx.tradeprocessor.audit;

import java.time.Instant;

import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;

/**
 * Immutable record representing a single auditable trade-processing event.
 * Fields align with the 7-year retention data-governance standard:
 * timestamp, tradeId, accountId, security, quantity, side, state, and initiator.
 */
public class AuditEvent {

    private final String eventType;
    private final Instant timestamp;
    private final String tradeId;
    private final Integer accountId;
    private final String security;
    private final Integer quantity;
    private final TradeSide side;
    private final TradeState currentState;
    private final TradeState previousState;
    private final String initiator;

    private AuditEvent(Builder builder) {
        this.eventType = builder.eventType;
        this.timestamp = builder.timestamp;
        this.tradeId = builder.tradeId;
        this.accountId = builder.accountId;
        this.security = builder.security;
        this.quantity = builder.quantity;
        this.side = builder.side;
        this.currentState = builder.currentState;
        this.previousState = builder.previousState;
        this.initiator = builder.initiator;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTradeId() {
        return tradeId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getSecurity() {
        return security;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public TradeSide getSide() {
        return side;
    }

    public TradeState getCurrentState() {
        return currentState;
    }

    public TradeState getPreviousState() {
        return previousState;
    }

    public String getInitiator() {
        return initiator;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventType;
        private Instant timestamp;
        private String tradeId;
        private Integer accountId;
        private String security;
        private Integer quantity;
        private TradeSide side;
        private TradeState currentState;
        private TradeState previousState;
        private String initiator;

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder tradeId(String tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        public Builder accountId(Integer accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder security(String security) {
            this.security = security;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder side(TradeSide side) {
            this.side = side;
            return this;
        }

        public Builder currentState(TradeState currentState) {
            this.currentState = currentState;
            return this;
        }

        public Builder previousState(TradeState previousState) {
            this.previousState = previousState;
            return this;
        }

        public Builder initiator(String initiator) {
            this.initiator = initiator;
            return this;
        }

        public AuditEvent build() {
            if (this.timestamp == null) {
                this.timestamp = Instant.now();
            }
            if (this.initiator == null) {
                this.initiator = "SYSTEM";
            }
            return new AuditEvent(this);
        }
    }
}
