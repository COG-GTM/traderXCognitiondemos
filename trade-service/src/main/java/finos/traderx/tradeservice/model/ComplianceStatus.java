package finos.traderx.tradeservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "The compliance review state of a trade order")
public enum ComplianceStatus {
    PENDING_REVIEW, APPROVED, FLAGGED, REJECTED
}
