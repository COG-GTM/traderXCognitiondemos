package finos.traderx.tradeservice.risk;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traderx.risk")
public class RiskLimitProperties {

    private final PreTradeChecks preTradeChecks = new PreTradeChecks();
    private final NotionalLimit notionalLimit = new NotionalLimit();
    private final Prices prices = new Prices();

    public PreTradeChecks getPreTradeChecks() {
        return preTradeChecks;
    }

    public NotionalLimit getNotionalLimit() {
        return notionalLimit;
    }

    public Prices getPrices() {
        return prices;
    }

    public static class PreTradeChecks {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Per-order notional limits. Sourced from configuration until account-service
     * owns them (TRX-102).
     */
    public static class NotionalLimit {
        private BigDecimal defaultLimit = new BigDecimal("1000000");
        private Map<Integer, BigDecimal> perAccount = new HashMap<>();

        public BigDecimal getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(BigDecimal defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        public Map<Integer, BigDecimal> getPerAccount() {
            return perAccount;
        }

        public void setPerAccount(Map<Integer, BigDecimal> perAccount) {
            this.perAccount = perAccount;
        }

        public BigDecimal limitFor(Integer accountId) {
            return perAccount.getOrDefault(accountId, defaultLimit);
        }
    }

    /**
     * Price fallbacks used while reference-data does not publish a last price.
     */
    public static class Prices {
        private BigDecimal fallbackLastPrice;
        private Map<String, BigDecimal> perTicker = new HashMap<>();

        public BigDecimal getFallbackLastPrice() {
            return fallbackLastPrice;
        }

        public void setFallbackLastPrice(BigDecimal fallbackLastPrice) {
            this.fallbackLastPrice = fallbackLastPrice;
        }

        public Map<String, BigDecimal> getPerTicker() {
            return perTicker;
        }

        public void setPerTicker(Map<String, BigDecimal> perTicker) {
            this.perTicker = perTicker;
        }
    }
}
