package finos.traderx.validationservice.model;

public class ValidationRequest {
    private String security;
    private Integer accountId;
    private String username;

    public ValidationRequest() {
    }

    public ValidationRequest(String security, Integer accountId, String username) {
        this.security = security;
        this.accountId = accountId;
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
