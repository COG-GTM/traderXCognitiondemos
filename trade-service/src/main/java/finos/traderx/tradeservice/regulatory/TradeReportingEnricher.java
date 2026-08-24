package finos.traderx.tradeservice.regulatory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import finos.traderx.tradeservice.model.TradeOrder;

/**
 * Stamps the regulatory reporting fields onto a trade order at the point of creation. Values
 * supplied by the submitting system are preserved so that externally generated UTIs still flow
 * through unchanged (and are still validated).
 */
@Service
public class TradeReportingEnricher {

    @Autowired
    private LeiRegistry leiRegistry;

    @Autowired
    private UtiGenerator utiGenerator;

    public TradeOrder enrich(TradeOrder order) {
        if (order == null) {
            return null;
        }
        if (isBlank(order.getReportingCounterpartyLei())) {
            order.setReportingCounterpartyLei(this.leiRegistry.leiFor(order.getAccountId()));
        }
        if (isBlank(order.getUti())) {
            order.setUti(this.utiGenerator.generate(order.getReportingCounterpartyLei()));
        }
        if (isBlank(order.getReportingRegime())) {
            order.setReportingRegime(RegulatoryRuleSet.EMIR_REFIT);
        }
        return order;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
