package finos.traderx.accountservice.exceptions;

public class RiskLimitsDisabledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RiskLimitsDisabledException(String message) {
		super(message);
	}
}
