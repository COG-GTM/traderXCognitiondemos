package finos.traderx.tradeprocessor.regulatory;

import java.util.regex.Pattern;

import finos.traderx.tradeprocessor.model.TradeOrder;

/**
 * Boundary check applied to trade orders arriving from the trade feed. The trade service owns
 * enrichment and the full EMIR REFIT rule set; the processor only refuses to book a trade it
 * could not report, so an unreportable record never reaches the trade store.
 */
public final class ReportingFieldGuard {

    public static final String EMIR_REFIT = "EMIR_REFIT";

    private static final Pattern LEI = Pattern.compile("^[A-Z0-9]{18}[0-9]{2}$");
    private static final Pattern UTI = Pattern.compile("^[A-Z0-9]{52}$");

    private ReportingFieldGuard() {
    }

    public static void check(TradeOrder order) {
        if (order.getUti() == null || !UTI.matcher(order.getUti()).matches()) {
            throw new UnreportableTradeException("REG-011", "uti",
                    "Trade order carries no valid 52 character UTI and cannot be booked");
        }
        if (order.getReportingCounterpartyLei() == null
                || !LEI.matcher(order.getReportingCounterpartyLei()).matches()) {
            throw new UnreportableTradeException("REG-021", "reportingCounterpartyLei",
                    "Trade order carries no valid 20 character reporting counterparty LEI and cannot be booked");
        }
        if (!order.getReportingCounterpartyLei().equals(order.getUti().substring(0, 20))) {
            throw new UnreportableTradeException("REG-013", "uti",
                    "UTI prefix does not match the reporting counterparty LEI");
        }
        if (!EMIR_REFIT.equals(order.getReportingRegime())) {
            throw new UnreportableTradeException("REG-031", "reportingRegime",
                    "Only " + EMIR_REFIT + " reportable trades can be booked");
        }
    }
}
