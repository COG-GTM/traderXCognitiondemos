package finos.traderx.itsupport;

import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Test-side Socket.IO client used to drive and observe the {@link EmbeddedTradeFeed}.
 * It can publish trade orders the way {@code trade-service} does and capture the
 * envelopes {@code trade-processor} publishes back onto the account topics.
 */
public class TradeFeedTestClient {

    private final Socket socket;
    private final List<JSONObject> received = new CopyOnWriteArrayList<>();

    public TradeFeedTestClient(String address) {
        this.socket = IO.socket(URI.create(address));
        this.socket.on("publish", args -> received.add((JSONObject) args[0]));
    }

    public TradeFeedTestClient connectAndSubscribe(EmbeddedTradeFeed feed, String... topics) {
        socket.connect();
        await().atMost(Duration.ofSeconds(10)).until(socket::connected);
        for (String topic : topics) {
            socket.emit("subscribe", topic);
        }
        for (String topic : topics) {
            await().atMost(Duration.ofSeconds(10)).until(() -> feed.subscriberCount(topic) >= 1);
        }
        return this;
    }

    /** Publishes a trade order envelope onto a topic, mimicking the trade-service publisher. */
    public void publishTradeOrder(String topic, JSONObject tradeOrder) {
        JSONObject envelope = new JSONObject()
                .put("topic", topic)
                .put("type", "TradeOrder")
                .put("payload", tradeOrder);
        socket.emit("publish", envelope);
    }

    public List<JSONObject> receivedOn(String topic) {
        return received.stream()
                .filter(e -> topic.equals(e.optString("topic")))
                .toList();
    }

    public void close() {
        socket.disconnect();
        socket.close();
    }
}
