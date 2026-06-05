package finos.traderx.accountservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import finos.traderx.accountservice.model.AccountTradeSummary;
import finos.traderx.accountservice.model.Trade;
import finos.traderx.accountservice.repository.TradeRepository;

/**
 * Builds account-level trade summaries that include the compliance state of the
 * account's trades.
 */
@Service
public class TradeSummaryService {

    private static final Logger log = LoggerFactory.getLogger(TradeSummaryService.class);

    @Autowired
    TradeRepository tradeRepository;

    public AccountTradeSummary getTradeSummaryForAccount(int accountId) {
        List<Trade> trades = this.tradeRepository.findByAccountId(accountId);
        AccountTradeSummary summary = summarize(accountId, trades);
        log.info("Trade summary for account {}: {} trades, compliance breakdown {}",
                accountId, summary.getTotalTrades(), summary.getComplianceBreakdown());
        return summary;
    }

    /**
     * Aggregates the given trades into a compliance-aware summary. Kept package
     * visible (public) so it can be unit tested without a database.
     */
    public AccountTradeSummary summarize(int accountId, List<Trade> trades) {
        AccountTradeSummary summary = new AccountTradeSummary(accountId);
        if (trades != null) {
            for (Trade trade : trades) {
                summary.record(trade.getComplianceStatus());
            }
        }
        return summary;
    }
}
