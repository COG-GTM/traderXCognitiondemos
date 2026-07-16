package finos.traderx.itsupport;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

/**
 * In-JVM Socket.IO server that reproduces the routing contract of the real
 * Node.js {@code trade-feed} ({@code trade-feed/index.js}): clients {@code subscribe}
 * to a topic (join a room) and {@code publish} an envelope which is re-wrapped and
 * broadcast to every subscriber of that topic (and the {@code /*} wildcard room).
 *
 * <p>This is a real Socket.IO bus (netty-socketio server + socket.io-client), not a
 * mocked {@code Publisher}, so tests exercise the same serialization and delivery path
 * the services use in production, while staying fully in-process, offline and
 * deterministic.
 *
 * <p>Every envelope received on {@code publish} is also captured server-side (see
 * {@link #publishedOn(String)}). Asserting on this capture verifies that a service
 * really published the event over the real socket to the real feed, without depending
 * on the timing of a second socket.io client receiving the re-broadcast.
 */
public class EmbeddedTradeFeed {

    private static final String WILDCARD_ROOM = "/*";

    private final SocketIOServer server;
    private final int port;
    private final List<Map<String, Object>> published = new CopyOnWriteArrayList<>();

    public EmbeddedTradeFeed() {
        this.port = findFreePort();
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(port);
        // Give the reactor enough threads for every client's concurrent polling +
        // websocket connections (the production publishers/subscriber default to the
        // polling->websocket transport); starving it drops messages silently. Generous
        // ping/upgrade timeouts keep connections stable for the whole test.
        config.setBossThreads(2);
        config.setWorkerThreads(16);
        config.setPingInterval(25000);
        config.setPingTimeout(120000);
        config.setUpgradeTimeout(30000);
        this.server = new SocketIOServer(config);
        wireHandlers();
    }

    private void wireHandlers() {
        server.addEventListener("subscribe", Object.class, (client, topic, ackRequest) -> {
            client.joinRoom(String.valueOf(topic));
            if (ackRequest != null && ackRequest.isAckRequested()) {
                ackRequest.sendAckData("subscribed:" + topic);
            }
        });

        server.addEventListener("unsubscribe", Object.class, (client, topic, ack) ->
                client.leaveRoom(String.valueOf(topic)));

        server.addEventListener("publish", Object.class, (client, data, ack) -> {
            if (!(data instanceof Map)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> incoming = (Map<String, Object>) data;
            String topic = String.valueOf(incoming.get("topic"));

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("type", incoming.get("type"));
            envelope.put("from", client.getSessionId().toString());
            envelope.put("topic", topic);
            envelope.put("date", System.currentTimeMillis());
            envelope.put("payload", incoming.get("payload"));

            published.add(envelope);
            server.getRoomOperations(topic).sendEvent("publish", envelope);
            server.getRoomOperations(WILDCARD_ROOM).sendEvent("publish", envelope);
        });
    }

    /** Envelopes captured server-side for a topic, as {@link JSONObject}s. */
    public List<JSONObject> publishedOn(String topic) {
        return published.stream()
                .filter(e -> topic.equals(e.get("topic")))
                .map(JSONObject::new)
                .toList();
    }

    /** Clears captured envelopes; call before each test since the feed is shared. */
    public void clearPublished() {
        published.clear();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return "http://localhost:" + port;
    }

    /** Number of clients currently joined to the given topic room. */
    public int subscriberCount(String topic) {
        return server.getRoomOperations(topic).getClients().size();
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException("Could not allocate a free port for the embedded trade-feed", e);
        }
    }
}
