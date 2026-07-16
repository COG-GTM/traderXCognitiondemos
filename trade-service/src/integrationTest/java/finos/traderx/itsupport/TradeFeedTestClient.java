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
 * Thin test-side Socket.IO subscriber used to observe what a service publishes onto
 * the {@link EmbeddedTradeFeed}. It captures raw {@code publish} envelopes per topic.
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

    public List<JSONObject> received() {
        return received;
    }

    /** Envelopes captured for a specific topic. */
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
