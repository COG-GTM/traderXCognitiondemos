package finos.traderx.tradeservice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

class PubSubConfigTest {

    @Test
    void tradePublisher_ReturnsNonNullPublisher() {
        PubSubConfig config = new PubSubConfig();
        ReflectionTestUtils.setField(config, "tradeFeedAddress", "http://localhost:18086");

        Publisher<TradeOrder> publisher = config.tradePublisher();

        assertNotNull(publisher);
    }

    @Test
    void tradePublisher_ReturnsPublisherInstance() {
        PubSubConfig config = new PubSubConfig();
        ReflectionTestUtils.setField(config, "tradeFeedAddress", "http://localhost:18086");

        Publisher<TradeOrder> publisher = config.tradePublisher();

        assertTrue(publisher instanceof Publisher);
    }

    @Test
    void tradePublisher_WithDifferentAddress_CreatesPublisher() {
        PubSubConfig config = new PubSubConfig();
        ReflectionTestUtils.setField(config, "tradeFeedAddress", "http://trade-feed:3000");

        Publisher<TradeOrder> publisher = config.tradePublisher();

        assertNotNull(publisher);
    }

    @Test
    void tradePublisher_IsNotConnectedInitially() {
        PubSubConfig config = new PubSubConfig();
        ReflectionTestUtils.setField(config, "tradeFeedAddress", "http://localhost:18086");

        Publisher<TradeOrder> publisher = config.tradePublisher();

        assertFalse(publisher.isConnected());
    }
}
