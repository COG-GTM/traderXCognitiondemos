package finos.traderx.accountservice.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Payload for PUT /account/{id}/risk-limit. Sent by the risk function, not by the desk.
 */
public class RiskLimitRequest {

	private BigDecimal maxOrderNotional;
	private String currency;
	private Date effectiveFrom;
	private String setBy;
	private String reason;

	public BigDecimal getMaxOrderNotional() {
		return this.maxOrderNotional;
	}

	public void setMaxOrderNotional(BigDecimal maxOrderNotional) {
		this.maxOrderNotional = maxOrderNotional;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Date getEffectiveFrom() {
		return this.effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public String getSetBy() {
		return this.setBy;
	}

	public void setSetBy(String setBy) {
		this.setBy = setBy;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
