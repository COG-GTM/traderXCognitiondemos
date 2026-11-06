package finos.traderx.tradeprocessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.socketio.SocketIOJSONSubscriber;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.service.TradeService;

public class TradeFeedHandler extends SocketIOJSONSubscriber<TradeOrder> {
    static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradeFeedHandler.class);

    public TradeFeedHandler(){
        super(TradeOrder.class);
    }
    @Autowired
    private TradeService tradeService;

    // trade-feed stamps "from" with the authenticated publisher, so only orders from trade-service are booked.
    @Value("${trade.feed.trusted.publisher:trade-service}")
    private String trustedPublisher = "trade-service";

    @Override
    public void onMessage(Envelope<?> envelope, TradeOrder order) {
        if (envelope == null || !trustedPublisher.equals(envelope.getFrom())) {
            log.warn("Ignoring trade order from untrusted publisher {}", envelope == null ? null : envelope.getFrom());
            return;
        }
        try{
            tradeService.processTrade(order);
        } catch (Exception x){
            log.error("Error processing trade order {} in envelope {}",order,envelope);
            log.error("Error handling incoming trade order:",x);
        }
       
    }
    
}
