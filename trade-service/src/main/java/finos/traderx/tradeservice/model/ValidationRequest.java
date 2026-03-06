package finos.traderx.tradeservice.model;

public class ValidationRequest {
    private int accountId;
    private String security;

    public ValidationRequest() {}

    public ValidationRequest(int accountId, String security) {
        this.accountId = accountId;
        this.security = security;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }
}
