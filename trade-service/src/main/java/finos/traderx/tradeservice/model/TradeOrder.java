package finos.traderx.tradeservice.model;

public record TradeOrder(String id, String state, String security, Integer quantity, Integer accountId,
        TradeSide side) {

    public TradeOrder(String id, int accountId, String security, TradeSide side, int quantity) {
        this(id, null, security, quantity, accountId, side);
    }
}
