package finos.traderx.tradeservice.regulatory;

/**
 * Machine readable rejection reason codes raised by the regulatory rule set.
 * Codes are part of the reporting contract and must remain stable across regime changes.
 */
public enum RejectionCode {

    ACCOUNT_MISSING("REG-001"),
    SECURITY_MISSING("REG-002"),
    SIDE_MISSING("REG-003"),
    QUANTITY_INVALID("REG-004"),
    UTI_MISSING("REG-010"),
    UTI_LENGTH_INVALID("REG-011"),
    UTI_FORMAT_INVALID("REG-012"),
    UTI_PREFIX_MISMATCH("REG-013"),
    LEI_MISSING("REG-020"),
    LEI_LENGTH_INVALID("REG-021"),
    LEI_FORMAT_INVALID("REG-022"),
    LEI_CHECKSUM_INVALID("REG-023"),
    REGIME_MISSING("REG-030"),
    REGIME_UNSUPPORTED("REG-031");

    private final String code;

    RejectionCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
