package finos.traderx.pnlservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import finos.traderx.pnlservice.messaging.socketio.SocketIOJSONPublisher;

@Component
public class PnlPublisher extends SocketIOJSONPublisher<PnlUpdate> {

    private static final Logger log = LoggerFactory.getLogger(PnlPublisher.class);

    public PnlPublisher(@Value("${trade.feed.address}") String tradeFeedAddress) {
        setSocketAddress(tradeFeedAddress);
        setTopic("/pnl");
    }

    public void publishPnlUpdate(Integer accountId, PnlUpdate update) {
        try {
            String topic = "/accounts/" + accountId + "/pnl";
            publish(topic, update);
            log.info("Published P&L update to topic {}", topic);
        } catch (PubSubException e) {
            log.error("Failed to publish P&L update for account {}", accountId, e);
        }
    }
}
