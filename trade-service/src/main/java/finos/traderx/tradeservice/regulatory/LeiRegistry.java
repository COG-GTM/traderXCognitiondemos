package finos.traderx.tradeservice.regulatory;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Resolves the reporting counterparty LEI for a trading account. In a production deployment this
 * is backed by the entity master; for the reference platform it is a static table so the
 * enrichment path can be exercised without external dependencies.
 */
@Service
public class LeiRegistry {

    public static final String DEFAULT_LEI = "549300TRADERXDFLT014";

    private static final Map<Integer, String> ACCOUNT_LEIS = Map.of(
            22214, "549300TRADERX0ACC128",
            11413, "549300TRADERX0ACC225",
            42422, "549300TRADERX0ACC322",
            52355, "549300TRADERX0ACC419",
            62654, "549300TRADERX0ACC516",
            10031, "549300TRADERX0ACC613",
            44044, "549300TRADERX0ACC710");

    public String leiFor(Integer accountId) {
        if (accountId == null) {
            return DEFAULT_LEI;
        }
        return ACCOUNT_LEIS.getOrDefault(accountId, DEFAULT_LEI);
    }
}
