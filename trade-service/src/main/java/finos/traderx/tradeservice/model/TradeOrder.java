package finos.traderx.tradeservice.model;

public class TradeOrder {

    public String id;
    private String state;
    private String security;
    private Integer quantity;
    private Integer accountId;
    private TradeSide side;
    private ComplianceStatus complianceStatus = ComplianceStatus.PENDING_REVIEW;

    public TradeOrder(){}
    
    public TradeOrder(String id, int accountId, String security, TradeSide side, int quantity) {
        this.accountId = accountId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.id = id;
    }

    public TradeOrder(String id, int accountId, String security, TradeSide side, int quantity, ComplianceStatus complianceStatus) {
        this(id, accountId, security, side, quantity);
        this.complianceStatus = complianceStatus;
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

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    @Override
    public String toString() {
        return "TradeOrder [id=" + id + ", state=" + state + ", security=" + security
                + ", quantity=" + quantity + ", accountId=" + accountId + ", side=" + side
                + ", complianceStatus=" + complianceStatus + "]";
    }
}
