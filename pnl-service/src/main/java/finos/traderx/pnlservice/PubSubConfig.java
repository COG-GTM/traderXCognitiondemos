package finos.traderx.pnlservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import finos.traderx.messaging.Publisher;
import finos.traderx.messaging.Subscriber;
import finos.traderx.messaging.socketio.SocketIOJSONPublisher;
import finos.traderx.pnlservice.model.PnlUpdate;
import finos.traderx.pnlservice.model.TradeUpdate;

@Configuration
public class PubSubConfig {
    @Value("${trade.feed.address}")
    private String tradeFeedAddress;

    @Bean 
    public Publisher<PnlUpdate> pnlPublisher() {
        SocketIOJSONPublisher<PnlUpdate> publisher = new SocketIOJSONPublisher<PnlUpdate>(){};
        publisher.setSocketAddress(tradeFeedAddress);
        return publisher;
    }

    @Bean 
    public Subscriber<TradeUpdate> tradeFeedHandler() {
        TradeFeedHandler handler = new TradeFeedHandler();
        handler.setDefaultTopic("/trades");
        handler.setSocketAddress(tradeFeedAddress);
        return handler;
    }
}
