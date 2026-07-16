package finos.traderx.itsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Spike / smoke test proving the {@link EmbeddedTradeFeed} (netty-socketio server)
 * and the production {@code io.socket:socket.io-client} speak the same protocol and
 * that the subscribe/publish routing contract works end to end.
 */
class EmbeddedTradeFeedConnectivityIT {

    private EmbeddedTradeFeed feed;
    private Socket socket;

    @BeforeEach
    void setUp() {
        feed = new EmbeddedTradeFeed();
        feed.start();
    }

    @AfterEach
    void tearDown() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
        feed.stop();
    }

    @Test
    void subscriberReceivesPublishedEnvelope() {
        List<JSONObject> received = new CopyOnWriteArrayList<>();

        socket = IO.socket(URI.create(feed.getAddress()));
        socket.on("publish", args -> received.add((JSONObject) args[0]));
        socket.connect();

        await().atMost(Duration.ofSeconds(10)).until(() -> socket.connected());

        socket.emit("subscribe", "/trades");
        await().atMost(Duration.ofSeconds(10)).until(() -> feed.subscriberCount("/trades") == 1);

        JSONObject payload = new JSONObject().put("id", "T-1").put("security", "IBM");
        JSONObject envelope = new JSONObject()
                .put("topic", "/trades")
                .put("type", "TradeOrder")
                .put("payload", payload);
        socket.emit("publish", envelope);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(received).hasSize(1));

        JSONObject env = received.get(0);
        assertThat(env.getString("type")).isEqualTo("TradeOrder");
        assertThat(env.getString("topic")).isEqualTo("/trades");
        assertThat(env.getJSONObject("payload").getString("id")).isEqualTo("T-1");
        assertThat(env.getJSONObject("payload").getString("security")).isEqualTo("IBM");
    }
}
