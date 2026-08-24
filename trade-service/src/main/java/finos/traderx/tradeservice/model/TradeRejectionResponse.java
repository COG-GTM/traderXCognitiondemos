package finos.traderx.tradeservice.model;

import java.util.List;

import finos.traderx.tradeservice.regulatory.RejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Machine readable rejection of a trade order that breaches the regulatory rule set")
public class TradeRejectionResponse {

    @Schema(name = "The reporting regime whose rules rejected the order", example = "EMIR_REFIT")
    private String reportingRegime;

    @Schema(name = "Every rule breach found on the submitted order")
    private List<RejectionReason> rejections;

    public TradeRejectionResponse() {
    }

    public TradeRejectionResponse(String reportingRegime, List<RejectionReason> rejections) {
        this.reportingRegime = reportingRegime;
        this.rejections = rejections;
    }

    public String getReportingRegime() {
        return this.reportingRegime;
    }

    public void setReportingRegime(String reportingRegime) {
        this.reportingRegime = reportingRegime;
    }

    public List<RejectionReason> getRejections() {
        return this.rejections;
    }

    public void setRejections(List<RejectionReason> rejections) {
        this.rejections = rejections;
    }
}
