package finos.traderx.messaging.socketio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import finos.traderx.messaging.Envelope;
import finos.traderx.tradeprocessor.model.TradeOrder;

class SocketIOJSONSubscriberTest {

    static class OrderSubscriber extends SocketIOJSONSubscriber<TradeOrder> {
        OrderSubscriber() {
            super(TradeOrder.class);
        }

        @Override
        public void onMessage(Envelope<?> envelope, TradeOrder message) {
        }
    }

    @Test
    void acceptsOnlyConfiguredPublishers() {
        OrderSubscriber subscriber = new OrderSubscriber();
        subscriber.setTrustedPublishers(Set.of("trade-service"));

        assertTrue(subscriber.isTrustedSender("trade-service"));
        assertFalse(subscriber.isTrustedSender("trade-processor"));
        assertFalse(subscriber.isTrustedSender("anonymous"));
        assertFalse(subscriber.isTrustedSender("hQx2Sxq0yJ9rT1AAAAB"));
        assertFalse(subscriber.isTrustedSender(null));
        assertFalse(subscriber.isTrustedSender(42));
    }
}
