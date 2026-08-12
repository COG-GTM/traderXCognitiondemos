package finos.traderx.positionservice.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the compliance query API (TRX-105).
 *
 * {@code enabled} is the single kill switch for the whole feature, on by default in dev. The
 * page sizes are configuration rather than constants because the sensible ceiling depends on
 * how much history a deployment actually holds.
 */
@ConfigurationProperties(prefix = "audit.query")
public class AuditQueryProperties {

    private boolean enabled = true;

    private int defaultPageSize = 50;

    private int maxPageSize = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
