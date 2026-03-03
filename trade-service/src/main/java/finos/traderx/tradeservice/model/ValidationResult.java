package finos.traderx.tradeservice.model;

public class ValidationResult {
    private boolean valid;
    private String reason;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
