package finos.traderx.pnlservice;

import org.springframework.beans.factory.annotation.Autowired;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.Publisher;
import finos.traderx.messaging.socketio.SocketIOJSONSubscriber;
import finos.traderx.pnlservice.model.PnlUpdate;
import finos.traderx.pnlservice.model.SecurityPnl;
import finos.traderx.pnlservice.model.TradeUpdate;
import finos.traderx.pnlservice.service.PnlService;

public class TradeFeedHandler extends SocketIOJSONSubscriber<TradeUpdate> {
    static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradeFeedHandler.class);

    public TradeFeedHandler(){
        super(TradeUpdate.class);
    }

    @Autowired
    private PnlService pnlService;

    @Autowired
    private Publisher<PnlUpdate> pnlPublisher;

    @Override
    public void onMessage(Envelope<?> envelope, TradeUpdate tradeUpdate) {
        try {
            if ("Settled".equals(tradeUpdate.getState())) {
                log.info("Processing settled trade for P&L update: {}", tradeUpdate.getId());
                
                SecurityPnl securityPnl = pnlService.getSecurityPnl(
                        tradeUpdate.getAccountId(), 
                        tradeUpdate.getSecurity());
                
                PnlUpdate pnlUpdate = new PnlUpdate(
                        tradeUpdate.getAccountId(),
                        tradeUpdate.getSecurity(),
                        securityPnl.getRealizedPnl(),
                        securityPnl.getUnrealizedPnl(),
                        "TRADE_SETTLED"
                );
                
                String topic = "/accounts/" + tradeUpdate.getAccountId() + "/pnl";
                pnlPublisher.publish(topic, pnlUpdate);
                log.info("Published P&L update to topic: {}", topic);
            }
        } catch (Exception x) {
            log.error("Error processing trade update {} in envelope {}", tradeUpdate, envelope);
            log.error("Error handling incoming trade update:", x);
        }
    }
}
