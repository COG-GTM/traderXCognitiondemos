package finos.traderx.tradeservice.regulatory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import finos.traderx.tradeservice.model.TradeOrder;

/**
 * Applies the regulatory rule table to a trade order. Every breached rule is reported so a
 * submitting system can fix the whole record in one pass.
 */
@Service
public class RegulatoryValidator {

    public ValidationResult validate(TradeOrder order) {
        List<RejectionReason> rejections = new ArrayList<>();
        for (RegulatoryRule rule : RegulatoryRuleSet.rules()) {
            if (rule.isBreachedBy(order)) {
                rejections.add(rule.toRejection());
            }
        }
        return new ValidationResult(rejections);
    }
}
