package finos.traderx.validationservice.model;

public class ValidationRequest {
    private String security;
    private Integer accountId;

    public ValidationRequest() {
    }

    public ValidationRequest(String security, Integer accountId) {
        this.security = security;
        this.accountId = accountId;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }
}
