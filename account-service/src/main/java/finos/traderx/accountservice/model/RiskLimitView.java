package finos.traderx.accountservice.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Response for GET /account/{id}/risk-limit.
 *
 * The response is always 200 for a known account. Callers must branch on limitPresent
 * rather than on the HTTP status, and when no limit is present they must honour
 * missingLimitPolicy - which is a configuration decision owned by compliance, not by
 * whichever service happens to be doing the enforcing.
 */
public class RiskLimitView {

	private int accountId;
	private boolean limitPresent;
	private MissingLimitPolicy missingLimitPolicy;
	private BigDecimal maxOrderNotional;
	private String currency;
	private Date effectiveFrom;
	private String setBy;
	private Date updated;

	public static RiskLimitView of(RiskLimit limit, MissingLimitPolicy missingLimitPolicy) {
		RiskLimitView view = new RiskLimitView();
		view.accountId = limit.getAccountId();
		view.limitPresent = true;
		view.missingLimitPolicy = missingLimitPolicy;
		view.maxOrderNotional = limit.getMaxOrderNotional();
		view.currency = limit.getCurrency();
		view.effectiveFrom = limit.getEffectiveFrom();
		view.setBy = limit.getSetBy();
		view.updated = limit.getUpdated();
		return view;
	}

	public static RiskLimitView absent(int accountId, MissingLimitPolicy missingLimitPolicy) {
		RiskLimitView view = new RiskLimitView();
		view.accountId = accountId;
		view.limitPresent = false;
		view.missingLimitPolicy = missingLimitPolicy;
		return view;
	}

	public int getAccountId() {
		return this.accountId;
	}

	public boolean isLimitPresent() {
		return this.limitPresent;
	}

	public MissingLimitPolicy getMissingLimitPolicy() {
		return this.missingLimitPolicy;
	}

	public BigDecimal getMaxOrderNotional() {
		return this.maxOrderNotional;
	}

	public String getCurrency() {
		return this.currency;
	}

	public Date getEffectiveFrom() {
		return this.effectiveFrom;
	}

	public String getSetBy() {
		return this.setBy;
	}

	public Date getUpdated() {
		return this.updated;
	}
}
