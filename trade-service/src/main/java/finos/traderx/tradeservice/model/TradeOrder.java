package finos.traderx.tradeservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class TradeOrder {

    public String id;
    private String state;
    private String security;
    private Integer quantity;
    private Integer accountId;
    private TradeSide side;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY,
            description = "Assigned by trade-service and ties the order to its audit record; any value sent by a client is replaced.")
    private String correlationId;

    public TradeOrder(){}
    
    public TradeOrder(String id, int accountId, String security, TradeSide side, int quantity) {
        this.accountId = accountId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
