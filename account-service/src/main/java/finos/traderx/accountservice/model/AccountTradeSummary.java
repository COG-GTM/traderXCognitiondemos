package finos.traderx.accountservice.model;

import java.util.EnumMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Account-level summary of trades broken down by their compliance state. This
 * exposes the new {@code complianceStatus} field on the shared TradeOrder
 * contract at the account level.
 */
@Schema(name = "Account-level summary of trades including their compliance state")
public class AccountTradeSummary {

    private int accountId;
    private int totalTrades;
    private Map<ComplianceStatus, Integer> complianceBreakdown = new EnumMap<>(ComplianceStatus.class);

    public AccountTradeSummary() {
        for (ComplianceStatus status : ComplianceStatus.values()) {
            this.complianceBreakdown.put(status, 0);
        }
    }

    public AccountTradeSummary(int accountId) {
        this();
        this.accountId = accountId;
    }

    /**
     * Records a single trade against this summary, incrementing the total and the
     * relevant compliance bucket. Trades with an unknown/null compliance status are
     * counted as PENDING_REVIEW.
     */
    public void record(String complianceStatus) {
        ComplianceStatus status;
        try {
            status = complianceStatus == null
                    ? ComplianceStatus.PENDING_REVIEW
                    : ComplianceStatus.valueOf(complianceStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            status = ComplianceStatus.PENDING_REVIEW;
        }
        this.complianceBreakdown.merge(status, 1, Integer::sum);
        this.totalTrades++;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(int totalTrades) {
        this.totalTrades = totalTrades;
    }

    public Map<ComplianceStatus, Integer> getComplianceBreakdown() {
        return complianceBreakdown;
    }

    public void setComplianceBreakdown(Map<ComplianceStatus, Integer> complianceBreakdown) {
        this.complianceBreakdown = complianceBreakdown;
    }
}
