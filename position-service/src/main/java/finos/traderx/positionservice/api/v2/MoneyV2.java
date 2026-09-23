package finos.traderx.positionservice.api.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Market value on the v2 contract: a decimal amount serialized as a STRING at the
 * currency's minor-unit scale, plus an explicit ISO-4217 currency code.
 */
public record MoneyV2(String amount, String currency) {

	private static final Map<String, Integer> SCALES = Map.of("JPY", 0, "KRW", 0);
	private static final int DEFAULT_SCALE = 2;

	public static int scaleFor(String currency) {
		return SCALES.getOrDefault(currency, DEFAULT_SCALE);
	}

	public static MoneyV2 of(BigDecimal amount, String currency) {
		if (amount == null || currency == null) {
			return null;
		}
		return new MoneyV2(amount.setScale(scaleFor(currency), RoundingMode.UNNECESSARY).toPlainString(), currency);
	}
}
