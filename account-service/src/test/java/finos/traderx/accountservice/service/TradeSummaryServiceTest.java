package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import finos.traderx.accountservice.model.AccountTradeSummary;
import finos.traderx.accountservice.model.ComplianceStatus;
import finos.traderx.accountservice.model.Trade;

class TradeSummaryServiceTest {

    private Trade tradeWith(String compliance) {
        Trade t = new Trade();
        t.setAccountId(22214);
        t.setComplianceStatus(compliance);
        return t;
    }

    @Test
    void summarizesTradesByComplianceState() {
        TradeSummaryService service = new TradeSummaryService();
        List<Trade> trades = Arrays.asList(
                tradeWith("APPROVED"),
                tradeWith("APPROVED"),
                tradeWith("FLAGGED"),
                tradeWith(null),
                tradeWith("not-a-status"));

        AccountTradeSummary summary = service.summarize(22214, trades);

        assertEquals(22214, summary.getAccountId());
        assertEquals(5, summary.getTotalTrades());
        assertEquals(2, summary.getComplianceBreakdown().get(ComplianceStatus.APPROVED));
        assertEquals(1, summary.getComplianceBreakdown().get(ComplianceStatus.FLAGGED));
        // null and unknown statuses both default to PENDING_REVIEW
        assertEquals(2, summary.getComplianceBreakdown().get(ComplianceStatus.PENDING_REVIEW));
        assertEquals(0, summary.getComplianceBreakdown().get(ComplianceStatus.REJECTED));
    }
}
