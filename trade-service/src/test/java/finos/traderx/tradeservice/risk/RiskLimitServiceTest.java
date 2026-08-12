package finos.traderx.tradeservice.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import finos.traderx.tradeservice.model.RiskDecision;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class RiskLimitServiceTest {

	private RiskLimitProperties properties;
	private RiskLimitService service;

	@BeforeEach
	void setUp() {
		properties = new RiskLimitProperties();
		properties.getNotionalLimit().setDefaultLimit(new BigDecimal("1000"));
		service = new RiskLimitService(properties);
	}

	private TradeOrder order(int quantity, TradeSide side) {
		return new TradeOrder("order-1", 42, "AAPL", side, quantity);
	}

	private Security priced(String price) {
		return new Security("AAPL", "Apple", new BigDecimal(price));
	}

	@Test
	void acceptsOrderWithinLimit() {
		RiskDecision decision = service.evaluate(order(5, TradeSide.Buy), priced("100"));

		assertFalse(decision.isRejected());
		assertEquals(RiskDecision.REASON_WITHIN_LIMIT, decision.getReason());
		assertEquals(0, decision.getAttempted().compareTo(new BigDecimal("500")));
	}

	@Test
	void acceptsOrderExactlyAtLimit() {
		RiskDecision decision = service.evaluate(order(10, TradeSide.Buy), priced("100"));

		assertFalse(decision.isRejected());
		assertEquals(0, decision.getAttempted().compareTo(new BigDecimal("1000")));
	}

	@Test
	void rejectsOrderOverLimit() {
		RiskDecision decision = service.evaluate(order(11, TradeSide.Buy), priced("100"));

		assertTrue(decision.isRejected());
		assertEquals(RiskDecision.REASON_NOTIONAL_LIMIT_BREACH, decision.getReason());
		assertEquals(0, decision.getLimit().compareTo(new BigDecimal("1000")));
		assertEquals(0, decision.getAttempted().compareTo(new BigDecimal("1100")));
	}

	@Test
	void rejectsSellOrderOverLimitOnAbsoluteNotional() {
		RiskDecision decision = service.evaluate(order(11, TradeSide.Sell), priced("100"));

		assertTrue(decision.isRejected());
		assertEquals(RiskDecision.REASON_NOTIONAL_LIMIT_BREACH, decision.getReason());
	}

	@Test
	void rejectsOrderWithNoPriceAvailable() {
		RiskDecision decision = service.evaluate(order(1, TradeSide.Buy), new Security("AAPL", "Apple"));

		assertTrue(decision.isRejected());
		assertEquals(RiskDecision.REASON_PRICE_UNAVAILABLE, decision.getReason());
		assertNull(decision.getAttempted());
	}

	@Test
	void fallsBackToConfiguredPriceWhenReferenceDataHasNone() {
		properties.getPrices().setFallbackLastPrice(new BigDecimal("50"));

		RiskDecision decision = service.evaluate(order(30, TradeSide.Buy), new Security("AAPL", "Apple"));

		assertTrue(decision.isRejected());
		assertEquals(RiskDecision.REASON_NOTIONAL_LIMIT_BREACH, decision.getReason());
		assertEquals(0, decision.getAttempted().compareTo(new BigDecimal("1500")));
	}

	@Test
	void perTickerPriceOverridesFallback() {
		properties.getPrices().setFallbackLastPrice(new BigDecimal("50"));
		properties.getPrices().getPerTicker().put("AAPL", new BigDecimal("10"));

		RiskDecision decision = service.evaluate(order(30, TradeSide.Buy), new Security("AAPL", "Apple"));

		assertFalse(decision.isRejected());
		assertEquals(0, decision.getAttempted().compareTo(new BigDecimal("300")));
	}

	@Test
	void perAccountLimitOverridesDefault() {
		properties.getNotionalLimit().getPerAccount().put(42, new BigDecimal("100"));

		RiskDecision decision = service.evaluate(order(5, TradeSide.Buy), priced("100"));

		assertTrue(decision.isRejected());
		assertEquals(0, decision.getLimit().compareTo(new BigDecimal("100")));
	}

	@Test
	void acceptsEverythingWhenFlagDisabled() {
		properties.getPreTradeChecks().setEnabled(false);

		RiskDecision decision = service.evaluate(order(1000000, TradeSide.Buy), new Security("AAPL", "Apple"));

		assertFalse(decision.isRejected());
		assertEquals(RiskDecision.REASON_CHECKS_DISABLED, decision.getReason());
	}
}
