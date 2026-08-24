package finos.traderx.tradeprocessor.regulatory;

/**
 * Raised when an incoming trade order is missing the regulatory reporting fields the trade store
 * requires. Carries the same rejection codes the trade service publishes on its API.
 */
public class UnreportableTradeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String field;

    public UnreportableTradeException(String code, String field, String message) {
        super(code + " " + field + " : " + message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return this.code;
    }

    public String getField() {
        return this.field;
    }
}
