package finos.traderx.tradeservice.exceptions;

import finos.traderx.tradeservice.model.RiskDecision;

public class PreTradeRiskException extends RuntimeException {

	private final RiskDecision decision;

	public PreTradeRiskException(RiskDecision decision) {
		super(decision.toString());
		this.decision = decision;
	}

	public RiskDecision getDecision() {
		return decision;
	}
}
