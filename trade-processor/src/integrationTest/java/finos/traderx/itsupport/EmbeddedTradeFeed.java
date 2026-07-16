package finos.traderx.itsupport;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

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
 */
public class EmbeddedTradeFeed {

    private static final String WILDCARD_ROOM = "/*";

    private final SocketIOServer server;
    private final int port;

    public EmbeddedTradeFeed() {
        this.port = findFreePort();
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(port);
        // Keep the reactor small and deterministic for tests.
        config.setBossThreads(1);
        config.setWorkerThreads(2);
        this.server = new SocketIOServer(config);
        wireHandlers();
    }

    private void wireHandlers() {
        server.addEventListener("subscribe", Object.class, (client, topic, ack) ->
                client.joinRoom(String.valueOf(topic)));

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

            server.getRoomOperations(topic).sendEvent("publish", envelope);
            server.getRoomOperations(WILDCARD_ROOM).sendEvent("publish", envelope);
        });
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
