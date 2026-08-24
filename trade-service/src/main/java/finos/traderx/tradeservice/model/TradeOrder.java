package finos.traderx.tradeservice.model;

public class TradeOrder {

    public String id;
    private String state;
    private String security;
    private Integer quantity;
    private Integer accountId;
    private TradeSide side;
    private String uti;
    private String reportingCounterpartyLei;
    private String reportingRegime;

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

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setSide(TradeSide side) {
        this.side = side;
    }

    public String getUti() {
        return uti;
    }

    public void setUti(String uti) {
        this.uti = uti;
    }

    public String getReportingCounterpartyLei() {
        return reportingCounterpartyLei;
    }

    public void setReportingCounterpartyLei(String reportingCounterpartyLei) {
        this.reportingCounterpartyLei = reportingCounterpartyLei;
    }

    public String getReportingRegime() {
        return reportingRegime;
    }

    public void setReportingRegime(String reportingRegime) {
        this.reportingRegime = reportingRegime;
    }

    @Override
    public String toString() {
        return "TradeOrder [id=" + id + ", accountId=" + accountId + ", security=" + security + ", side=" + side
                + ", quantity=" + quantity + ", state=" + state + ", uti=" + uti + ", reportingCounterpartyLei="
                + reportingCounterpartyLei + ", reportingRegime=" + reportingRegime + "]";
    }
}
