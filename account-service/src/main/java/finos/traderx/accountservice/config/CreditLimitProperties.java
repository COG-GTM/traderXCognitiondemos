package finos.traderx.accountservice.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable per-account credit limits exposed as account metadata.
 * A sensible global {@code defaultLimit} is applied to every account, with
 * optional per-account overrides keyed by account id.
 */
@Component
@ConfigurationProperties(prefix = "account.credit-limit")
public class CreditLimitProperties {

    private Long defaultLimit = 250000L;

    private Map<Integer, Long> overrides = new HashMap<>();

    public Long getDefaultLimit() {
        return this.defaultLimit;
    }

    public void setDefaultLimit(Long defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Map<Integer, Long> getOverrides() {
        return this.overrides;
    }

    public void setOverrides(Map<Integer, Long> overrides) {
        this.overrides = overrides;
    }

    /**
     * Resolve the credit limit for a given account id, using the per-account
     * override when present and falling back to the configured default.
     */
    public Long resolveFor(int accountId) {
        return this.overrides.getOrDefault(accountId, this.defaultLimit);
    }
}
