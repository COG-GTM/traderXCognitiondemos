package finos.traderx.tradeservice.audit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

/**
 * Externalised configuration for the best-execution audit trail. Nothing here is hard-coded
 * in the decision path; see application.properties for the dev defaults.
 */
@ConfigurationProperties(prefix = "audit.best-execution")
public class BestExecutionAuditProperties {

    private static final int COLUMN_SCALE = 4;
    private static final int NOTIONAL_PRECISION = 23;
    private static final int PRICE_PRECISION = 19;

    /** Kill switch for the whole feature. Defaults to on in dev. */
    private boolean enabled = true;

    private final Limit limit = new Limit();

    private final Pricing pricing = new Pricing();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Limit getLimit() {
        return limit;
    }

    public Pricing getPricing() {
        return pricing;
    }

    /**
     * Unlike the string values, an over-large number cannot be fitted to its column without
     * changing what the record says, so a misconfiguration is refused at startup rather than
     * discovered when the first order fails to be recorded.
     */
    @PostConstruct
    void validate() {
        // Normalised to the stored scale first, so the recorded notional is exactly the
        // recorded price times quantity rather than the rounding of an unrecorded price.
        if (pricing.getReferencePrice() != null) {
            pricing.setReferencePrice(pricing.getReferencePrice().setScale(COLUMN_SCALE, RoundingMode.HALF_UP));
        }
        if (limit.getValue() != null) {
            limit.setValue(limit.getValue().setScale(COLUMN_SCALE, RoundingMode.HALF_UP));
        }
        requireStorable("audit.best-execution.limit.value", limit.getValue(), NOTIONAL_PRECISION);
        requireStorable("audit.best-execution.pricing.reference-price", pricing.getReferencePrice(),
                PRICE_PRECISION);
        // The stored notional is price x quantity, so the price has to fit the notional column
        // at the largest quantity the order model can carry, not merely fit its own column.
        BigDecimal price = requireSet("audit.best-execution.pricing.reference-price", pricing.getReferencePrice());
        requireStorable("audit.best-execution.pricing.reference-price (x max quantity)",
                price.multiply(BigDecimal.valueOf(Integer.MAX_VALUE)),
                NOTIONAL_PRECISION);
    }

    private static BigDecimal requireSet(String property, BigDecimal value) {
        if (value == null) {
            throw new IllegalStateException(property + " must be set for the best-execution audit trail.");
        }
        return value;
    }

    private static void requireStorable(String property, BigDecimal value, int precision) {
        BigDecimal scaled = requireSet(property, value).setScale(COLUMN_SCALE, RoundingMode.HALF_UP);
        if (scaled.precision() - scaled.scale() > precision - COLUMN_SCALE) {
            throw new IllegalStateException(property + " does not fit the audit column DECIMAL(" + precision + ","
                    + COLUMN_SCALE + "): " + value);
        }
    }

    /**
     * The limit snapshot recorded against each decision until TRX-102 provides a real
     * limits store to read from.
     */
    public static class Limit {
        private String id = "DEFAULT-ACCOUNT-NOTIONAL";
        private String type = "ACCOUNT_NOTIONAL";
        private BigDecimal value = new BigDecimal("5000000.0000");
        private Instant effectiveFrom = Instant.parse("2024-01-01T00:00:00Z");

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public Instant getEffectiveFrom() {
            return effectiveFrom;
        }

        public void setEffectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
        }
    }

    /**
     * TraderX has no market data service, so the price used to compute notional comes from
     * configuration and is labelled as such in every record.
     */
    public static class Pricing {
        private BigDecimal referencePrice = new BigDecimal("100.0000");
        private String source = "CONFIGURED_STATIC_REFERENCE_PRICE";

        public BigDecimal getReferencePrice() {
            return referencePrice;
        }

        public void setReferencePrice(BigDecimal referencePrice) {
            this.referencePrice = referencePrice;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
