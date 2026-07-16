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
 * Order-flow end-to-end (S1 -> S2 -> S3 data seam): a trade order is submitted exactly as
 * {@code trade-service} publishes it, travels the real Socket.IO {@code trade-feed}, is
 * consumed by {@code trade-processor}, settled and persisted to the real H2 database, and
 * the resulting position - the same row {@code position-service} serves - reflects the
 * trade. All waits are bounded via Awaitility (no {@code Thread.sleep}).
 *
 * <p>The REST entry point of {@code trade-service} is exercised over the same real bus in
 * {@code trade-service}'s {@code TradeOrderControllerIT}; {@code position-service}'s read of
 * this identical schema is verified in its {@code PositionQueryIT}.
 */
class OrderFlowE2EIT extends AbstractTradeProcessorIT {

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
    void orderSubmittedToFeedIsSettledPersistedAndReflectedInPosition() {
        // Seeded starting position: account 22214 holds -100 IBM.
        assertThat(positionRepository.findByAccountIdAndSecurity(ACCOUNT, "IBM").getQuantity())
                .isEqualTo(-100);

        client = new TradeFeedTestClient(tradeFeed.getAddress()).connectAndSubscribe(tradeFeed);
        await().atMost(Duration.ofSeconds(20)).until(() -> tradeFeed.subscriberCount("/trades") >= 1);
        await().atMost(Duration.ofSeconds(20)).until(tradePublisher::isConnected);
        await().atMost(Duration.ofSeconds(20)).until(positionPublisher::isConnected);

        JSONObject order = new JSONObject()
                .put("id", "E2E-IBM-1")
                .put("security", "IBM")
                .put("quantity", 100)
                .put("accountId", ACCOUNT)
                .put("side", "Sell");
        client.publishTradeOrder("/trades", order);

        // S2: trade is persisted and settled in the real database.
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(newTrades()).hasSize(1));
        Trade settled = newTrades().get(0);
        assertThat(settled.getState()).isEqualTo(TradeState.Settled);
        assertThat(settled.getSide().name()).isEqualTo("Sell");
        assertThat(settled.getQuantity()).isEqualTo(100);

        // S3 data seam: the persisted position (what position-service serves) reflects the sell.
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(
                        positionRepository.findByAccountIdAndSecurity(ACCOUNT, "IBM").getQuantity())
                        .isEqualTo(-200));

        // Outbound trade/position events are published for downstream consumers
        // (asserted from the feed's server-side capture).
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertThat(tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/trades")).hasSize(1);
                    assertThat(tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/positions")).hasSize(1);
                });
        assertThat(tradeFeed.publishedOn("/accounts/" + ACCOUNT + "/positions").get(0)
                .getJSONObject("payload").getInt("quantity")).isEqualTo(-200);
    }

    private List<Trade> newTrades() {
        // Seeded IBM trade for 22214 is 'TRADE-22214-AABBCC'; the new one has a random UUID.
        return tradeRepository.findByAccountId(ACCOUNT).stream()
                .filter(t -> "IBM".equals(t.getSecurity()))
                .filter(t -> !t.getId().startsWith("TRADE-"))
                .toList();
    }
}
