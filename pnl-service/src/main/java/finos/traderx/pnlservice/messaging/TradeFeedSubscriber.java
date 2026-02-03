package finos.traderx.pnlservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import finos.traderx.pnlservice.messaging.socketio.SocketIOJSONSubscriber;
import finos.traderx.pnlservice.model.PnlSummary;
import finos.traderx.pnlservice.service.PnlService;

@Component
public class TradeFeedSubscriber extends SocketIOJSONSubscriber<TradeMessage> {

    private static final Logger log = LoggerFactory.getLogger(TradeFeedSubscriber.class);

    @Autowired
    private PnlService pnlService;

    @Autowired
    private PnlPublisher pnlPublisher;

    public TradeFeedSubscriber(@Value("${trade.feed.address}") String tradeFeedAddress) {
        super(TradeMessage.class);
        setSocketAddress(tradeFeedAddress);
        setDefaultTopic("/trades");
    }

    @Override
    public void onMessage(Envelope<?> envelope, TradeMessage message) {
        log.info("Received trade message for account {}: {} {} {} shares of {}",
                message.getAccountId(), message.getSide(), message.getQuantity(),
                message.getSecurity(), message.getState());

        if ("Settled".equals(message.getState())) {
            try {
                PnlSummary summary = pnlService.getPnlSummary(message.getAccountId());
                
                PnlUpdate update = new PnlUpdate(
                    message.getAccountId(),
                    summary.getRealizedPnl(),
                    summary.getUnrealizedPnl(),
                    message.getSecurity(),
                    "TRADE_SETTLED"
                );

                pnlPublisher.publishPnlUpdate(message.getAccountId(), update);
                log.info("Published P&L update for account {}", message.getAccountId());
            } catch (Exception e) {
                log.error("Error processing trade message and publishing P&L update", e);
            }
        }
    }
}
