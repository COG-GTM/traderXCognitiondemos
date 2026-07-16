package finos.traderx.tradeprocessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import finos.traderx.itsupport.TradeFeedTestClient;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeState;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

/**
 * S2: {@code trade-feed} -> {@code trade-processor} -> H2. A trade order published onto
 * the real Socket.IO feed is consumed, persisted to the real database and re-published
 * onto the account topics. The outbound events are asserted from the feed's server-side
 * capture (the processor really emitted them over the real socket).
 */
class TradeProcessorConsumerIT extends AbstractTradeProcessorIT {

    private static final int ACCOUNT = 22214;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private Publisher<Trade> tradePublisher;

    @Autowired
    private Publisher<Position> positionPublisher;

    private TradeFeedTestClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void publishedOrderIsPersistedSettledAndRepublished() {
        client = new TradeFeedTestClient(tradeFeed.getAddress()).connectAndSubscribe(tradeFeed);
        awaitBusReady();

        JSONObject order = new JSONObject()
                .put("id", "ORD-AAPL-1")
                .put("security", "AAPL")
                .put("quantity", 50)
                .put("accountId", ACCOUNT)
                .put("side", "Buy");
        client.publishTradeOrder("/trades", order);

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(tradesFor(ACCOUNT, "AAPL")).hasSize(1));

        Trade persisted = tradesFor(ACCOUNT, "AAPL").get(0);
        assertThat(persisted.getState()).isEqualTo(TradeState.Settled);
        assertThat(persisted.getQuantity()).isEqualTo(50);
        assertThat(persisted.getSide().name()).isEqualTo("Buy");

        Position position = positionRepository.findByAccountIdAndSecurity(ACCOUNT, "AAPL");
        assertThat(position).isNotNull();
        assertThat(position.getQuantity()).isEqualTo(50);

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertThat(tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/trades")).hasSize(1);
                    assertThat(tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/positions")).hasSize(1);
                });

        JSONObject tradeEvent = tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/trades").get(0)
                .getJSONObject("payload");
        assertThat(tradeEvent.getString("security")).isEqualTo("AAPL");
        assertThat(tradeEvent.getString("state")).isEqualTo("Settled");

        JSONObject positionEvent = tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/positions").get(0)
                .getJSONObject("payload");
        assertThat(positionEvent.getString("security")).isEqualTo("AAPL");
        assertThat(positionEvent.getInt("quantity")).isEqualTo(50);
    }

    private void awaitBusReady() {
        await().atMost(Duration.ofSeconds(20)).until(() -> tradeFeed.subscriberCount("/trades") >= 1);
        await().atMost(Duration.ofSeconds(20)).until(tradePublisher::isConnected);
        await().atMost(Duration.ofSeconds(20)).until(positionPublisher::isConnected);
    }

    private List<Trade> tradesFor(int account, String security) {
        return tradeRepository.findByAccountId(account).stream()
                .filter(t -> security.equals(t.getSecurity()))
                .toList();
    }
}
