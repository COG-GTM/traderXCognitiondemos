package finos.traderx.tradeservice.regulatory;

import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final List<RejectionReason> rejections;

    public ValidationResult(List<RejectionReason> rejections) {
        this.rejections = Collections.unmodifiableList(rejections);
    }

    public boolean isValid() {
        return this.rejections.isEmpty();
    }

    public List<RejectionReason> getRejections() {
        return this.rejections;
    }

    public List<String> getRejectionCodes() {
        return this.rejections.stream().map(RejectionReason::getCode).toList();
    }
}
