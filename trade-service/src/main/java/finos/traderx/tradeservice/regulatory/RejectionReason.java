package finos.traderx.tradeservice.regulatory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "A single regulatory rule breach on a submitted trade order")
public class RejectionReason {

    @Schema(name = "Stable machine readable rejection code", example = "REG-021")
    private String code;

    @Schema(name = "The trade order field the rule applies to", example = "reportingCounterpartyLei")
    private String field;

    @Schema(name = "Human readable explanation of the breach")
    private String message;

    public RejectionReason() {
    }

    public RejectionReason(RejectionCode code, String field, String message) {
        this.code = code.getCode();
        this.field = field;
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getField() {
        return this.field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.code + " " + this.field + " : " + this.message;
    }
}
