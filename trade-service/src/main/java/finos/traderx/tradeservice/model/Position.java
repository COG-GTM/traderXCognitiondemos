package finos.traderx.tradeservice.model;

public class Position {
    private Integer accountId;
    private String security;
    private Integer quantity;

    public Position() {
    }

    public Position(Integer accountId, String security, Integer quantity) {
        this.accountId = accountId;
        this.security = security;
        this.quantity = quantity;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
