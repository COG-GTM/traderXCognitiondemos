package finos.traderx.tradeprocessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import finos.traderx.itsupport.TradeFeedTestClient;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.repository.PositionRepository;

/**
 * S2 position maths: buys add, sells subtract, brand-new positions start at zero and
 * existing (seeded) positions are upserted. Every case flows through the real feed,
 * processor and H2 database.
 */
class PositionMathIT extends AbstractTradeProcessorIT {

    private static final int ACCOUNT = 22214;

    @Autowired
    private PositionRepository positionRepository;

    private TradeFeedTestClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void buyOnNewSecurityStartsFromZero() {
        connect();
        publish("AAPL", "Buy", 50);
        awaitQuantity("AAPL", 50);
    }

    @Test
    void sellOnNewSecurityGoesNegative() {
        connect();
        publish("GOOG", "Sell", 40);
        awaitQuantity("GOOG", -40);
    }

    @Test
    void tradesUpsertExistingSeededPosition() {
        // Seeded position: account 22214 holds 1000 MS.
        connect();
        assertThat(positionRepository.findByAccountIdAndSecurity(ACCOUNT, "MS").getQuantity())
                .isEqualTo(1000);

        publish("MS", "Buy", 500);
        awaitQuantity("MS", 1500);

        publish("MS", "Sell", 200);
        awaitQuantity("MS", 1300);
    }

    private void connect() {
        client = new TradeFeedTestClient(tradeFeed.getAddress()).connectAndSubscribe(tradeFeed);
        await().atMost(Duration.ofSeconds(10)).until(() -> tradeFeed.subscriberCount("/trades") >= 1);
    }

    private void publish(String security, String side, int quantity) {
        JSONObject order = new JSONObject()
                .put("id", "ORD-" + security + "-" + side + "-" + quantity)
                .put("security", security)
                .put("quantity", quantity)
                .put("accountId", ACCOUNT)
                .put("side", side);
        client.publishTradeOrder("/trades", order);
    }

    private void awaitQuantity(String security, int expected) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Position p = positionRepository.findByAccountIdAndSecurity(ACCOUNT, security);
                    assertThat(p).isNotNull();
                    assertThat(p.getQuantity()).isEqualTo(expected);
                });
    }
}
