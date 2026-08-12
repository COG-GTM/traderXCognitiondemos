package finos.traderx.tradeservice.risk;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import finos.traderx.tradeservice.model.RiskDecision;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;

/**
 * Pre-trade notional check (MiFID II RTS 6 Art. 15). Evaluates a single order
 * in isolation: absolute notional, no netting against existing or in-flight
 * positions.
 */
@Service
public class RiskLimitService {

	private static final Logger log = LoggerFactory.getLogger(RiskLimitService.class);

	private final RiskLimitProperties properties;

	public RiskLimitService(RiskLimitProperties properties) {
		this.properties = properties;
	}

	public RiskDecision evaluate(TradeOrder tradeOrder, Security security) {
		if (!properties.getPreTradeChecks().isEnabled()) {
			return RiskDecision.accepted(RiskDecision.REASON_CHECKS_DISABLED, null, null);
		}

		BigDecimal limit = properties.getNotionalLimit().limitFor(tradeOrder.getAccountId());

		if (tradeOrder.getQuantity() == null || tradeOrder.getQuantity() == 0) {
			log.info("Order {} has no usable quantity; cannot evaluate notional.", tradeOrder.getId());
			return RiskDecision.rejected(RiskDecision.REASON_INVALID_QUANTITY, limit, null);
		}

		Optional<BigDecimal> price = lastPriceFor(security, tradeOrder.getSecurity());

		if (price.isEmpty()) {
			log.info("No price available for {}; rejecting order as un-priceable.", tradeOrder.getSecurity());
			return RiskDecision.rejected(RiskDecision.REASON_PRICE_UNAVAILABLE, limit, null);
		}

		BigDecimal quantity = BigDecimal.valueOf(tradeOrder.getQuantity()).abs();
		BigDecimal attempted = price.get().multiply(quantity);

		if (attempted.compareTo(limit) > 0) {
			log.info("Order {} breaches notional limit: attempted {} vs limit {}", tradeOrder.getId(), attempted, limit);
			return RiskDecision.rejected(RiskDecision.REASON_NOTIONAL_LIMIT_BREACH, limit, attempted);
		}
		return RiskDecision.accepted(RiskDecision.REASON_WITHIN_LIMIT, limit, attempted);
	}

	private Optional<BigDecimal> lastPriceFor(Security security, String ticker) {
		if (security != null && security.getLastPrice() != null) {
			return Optional.of(security.getLastPrice());
		}
		RiskLimitProperties.Prices prices = properties.getPrices();
		BigDecimal configured = prices.getPerTicker().get(ticker);
		if (configured == null) {
			configured = prices.getFallbackLastPrice();
		}
		return Optional.ofNullable(configured);
	}
}
