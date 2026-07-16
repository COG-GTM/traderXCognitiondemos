package finos.traderx.itsupport;

import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Test-side Socket.IO client used to drive and observe the {@link EmbeddedTradeFeed}.
 * It can publish trade orders the way {@code trade-service} does and capture the
 * envelopes {@code trade-processor} publishes back onto the account topics.
 *
 * <p>Subscriptions are confirmed via a server ack so a test never proceeds until this
 * client has actually joined its rooms (the shared feed makes subscriber counts an
 * unreliable readiness signal), and the client uses the websocket transport only to
 * avoid losing messages during a polling->websocket upgrade.
 */
public class TradeFeedTestClient {

    private final Socket socket;
    private final List<JSONObject> received = new CopyOnWriteArrayList<>();
    private final List<String> subscribedTopics = new ArrayList<>();
    private EmbeddedTradeFeed feed;

    public TradeFeedTestClient(String address) {
        IO.Options options = new IO.Options();
        options.transports = new String[] {"websocket"};
        this.socket = IO.socket(URI.create(address), options);
        this.socket.on("publish", args -> received.add((JSONObject) args[0]));
    }

    public TradeFeedTestClient connectAndSubscribe(EmbeddedTradeFeed feed, String... topics) {
        this.feed = feed;
        socket.connect();
        await().atMost(Duration.ofSeconds(20)).until(socket::connected);
        for (String topic : topics) {
            AtomicBoolean acked = new AtomicBoolean(false);
            socket.emit("subscribe", new Object[] {topic}, args -> acked.set(true));
            await().atMost(Duration.ofSeconds(20)).untilTrue(acked);
            subscribedTopics.add(topic);
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
        // Wait until the server has fully removed this client from its rooms, so a
        // lingering (disconnected) session can never interfere with a later test's
        // broadcast on the same shared feed.
        if (feed != null) {
            for (String topic : subscribedTopics) {
                await().atMost(Duration.ofSeconds(20)).until(() -> feed.subscriberCount(topic) == 0);
            }
        }
    }
}
