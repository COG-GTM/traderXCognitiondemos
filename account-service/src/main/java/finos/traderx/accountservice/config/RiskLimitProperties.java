package finos.traderx.accountservice.config;

import finos.traderx.accountservice.model.MissingLimitPolicy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "traderx.risk-limit")
public class RiskLimitProperties {

	/** Master switch for the risk limit feature. Off means the service behaves as it did before TRX-102. */
	private boolean enabled = true;

	/** How callers should treat an account with no limit on file. */
	private MissingLimitPolicy missingLimitPolicy = MissingLimitPolicy.UNLIMITED;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public MissingLimitPolicy getMissingLimitPolicy() {
		return this.missingLimitPolicy;
	}

	public void setMissingLimitPolicy(MissingLimitPolicy missingLimitPolicy) {
		this.missingLimitPolicy = missingLimitPolicy;
	}
}
