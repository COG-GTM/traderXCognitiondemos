package finos.traderx.tradeprocessor.service;

import java.util.ArrayList;
import java.util.List;

import org.mockito.stubbing.Answer;

import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;

/**
 * Helpers shared by the trade-processor lifecycle tests.
 */
final class TradeServiceTestSupport {

	private TradeServiceTestSupport() {
	}

	static TradeOrder order(Integer accountId, String security, TradeSide side, Integer quantity) {
		TradeOrder order = new TradeOrder();
		set(order, "accountId", accountId);
		set(order, "security", security);
		set(order, "side", side);
		set(order, "quantity", quantity);
		return order;
	}

	static TradeOrder order(String id, Integer accountId, String security, TradeSide side, Integer quantity) {
		TradeOrder order = order(accountId, security, side, quantity);
		order.id = id;
		return order;
	}

	static Position position(Integer accountId, String security, int quantity) {
		Position p = new Position();
		p.setAccountId(accountId);
		p.setSecurity(security);
		p.setQuantity(quantity);
		return p;
	}

	/**
	 * Records the state of every Trade at the moment it is handed to save(), so
	 * that mutations applied after the call cannot hide the persisted value.
	 */
	static Answer<Trade> recordTradeStates(List<TradeState> sink) {
		return invocation -> {
			Trade saved = invocation.getArgument(0);
			sink.add(saved.getState());
			return saved;
		};
	}

	static Answer<Position> recordPositionQuantities(List<Integer> sink) {
		return invocation -> {
			Position saved = invocation.getArgument(0);
			sink.add(saved.getQuantity());
			return saved;
		};
	}

	static List<TradeState> newStateSink() {
		return new ArrayList<>();
	}

	private static void set(Object target, String field, Object value) {
		try {
			java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot set " + field, e);
		}
	}
}
