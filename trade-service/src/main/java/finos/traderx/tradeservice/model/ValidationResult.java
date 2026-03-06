package finos.traderx.tradeservice.model;

public class ValidationResult {
    private boolean valid;
    private String reason;

    public static ValidationResult valid() {
        ValidationResult r = new ValidationResult();
        r.setValid(true);
        return r;
    }

    public static ValidationResult invalid(String reason) {
        ValidationResult r = new ValidationResult();
        r.setValid(false);
        r.setReason(reason);
        return r;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
