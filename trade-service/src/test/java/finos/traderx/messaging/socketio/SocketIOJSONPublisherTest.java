package finos.traderx.messaging.socketio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class SocketIOJSONPublisherTest {

	@Test
	void refusesToPublishWhileDisconnected() {
		SocketIOJSONPublisher<TradeOrder> publisher = new SocketIOJSONPublisher<>() {
		};
		publisher.setTopic("/trades");
		TradeOrder order = new TradeOrder("trade-1", 1, "IBM", TradeSide.Buy, 10);

		assertThat(publisher.isConnected()).isFalse();
		assertThatThrownBy(() -> publisher.publish(order))
				.isInstanceOf(PubSubException.class)
				.hasMessageContaining("not connected")
				.hasMessageContaining("/trades");
	}
}
