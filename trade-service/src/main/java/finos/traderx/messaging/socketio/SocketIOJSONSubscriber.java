package finos.traderx.messaging.socketio;

import java.net.URI;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Subscriber;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Simple socketIO Subscriber, which uses 3 commands - 'subscribe',
 * 'unsubscribe', and 'publish' followed by payload
 * Publish events consist of an envelope and an internal payload.
 */
public abstract class SocketIOJSONSubscriber<T> implements Subscriber<T>, InitializingBean {
    private static ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public SocketIOJSONSubscriber(Class<T> typeClass) {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(SocketIOEnvelope.class, typeClass );
        this.envelopeType = type;
        this.objectType = typeClass;
    }

    private String authToken;

    /** Service token presented in the Socket.IO handshake ({@code auth.token}). */
    public void setAuthToken(String token) {
        authToken = token;
    }

    protected IO.Options getIOOptions() {
        IO.Options options = new IO.Options();
        if (authToken != null && !authToken.isBlank()) {
            options.auth = java.util.Map.of("token", authToken);
        }
        return options;
    }

    final JavaType envelopeType;
    final Class<T> objectType;

    org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass().getName());

    boolean connected = false;

    @Override
    public boolean isConnected() {
        return connected;
    }

    Socket socket;

    String socketAddress = "http://localhost:3000";

    public void setSocketAddress(String addr) {
        socketAddress = addr;
    }

    private java.util.Set<String> trustedPublishers = java.util.Set.of();

    /**
     * Sender identities (the server-stamped envelope {@code from}) whose messages are accepted.
     * When empty, typed messages are accepted from any sender.
     */
    public void setTrustedPublishers(java.util.Set<String> publishers) {
        trustedPublishers = publishers == null ? java.util.Set.of() : java.util.Set.copyOf(publishers);
    }

    boolean isTrustedSender(Object from) {
        return trustedPublishers.isEmpty() || (from instanceof String && trustedPublishers.contains(from));
    }

    private String defaultTopic = "/default";
    public void setDefaultTopic(String topic) {
        defaultTopic = topic;
    }

    public abstract void onMessage(Envelope<?> envelope, T message);

    @Override
    public void subscribe(String topic) throws PubSubException {
        log.info("Subscribing to " + topic);
        socket.emit("subscribe", topic);
    }

    @Override
    public void unsubscribe(String topic) throws PubSubException {
        socket.emit("unsubscribe", topic);
    }

    @Override
    public void disconnect() throws PubSubException {
        if (socket != null && isConnected())
            socket.disconnect();
        socket = null;
    }

    @Override
    public void connect() throws PubSubException {
        if (socket != null)
            socket.disconnect();
        try {
            socket = internalConnect(URI.create(socketAddress));
        } catch (Exception x) {
            throw new PubSubException("Cannot socket connection at " + socketAddress, x);
        }
    }

    protected Socket internalConnect(URI uri) throws Exception {
        Socket s = IO.socket(uri, getIOOptions());
        s.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = true;
                log.info("Socket Connected");
            }
        });

        s.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = false;
                log.info("Socket Disconnected");
            }
        });

        s.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = false;
                log.info("Connection Error");
            }
        });

        s.on("publish", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject json = (JSONObject) args[0]; 
                    log.info("Raw Payload " + args[0].toString());
                    if(! objectType.getSimpleName().equals(json.opt("type"))){
                        log.info("System Message>>>>> " + args[0].toString());
                    } else if (!isTrustedSender(json.opt("from"))) {
                        log.warn("Dropping {} from untrusted sender {}", objectType.getSimpleName(), json.opt("from"));
                    } else {
                        SocketIOEnvelope<T> envelope = (SocketIOEnvelope<T>) objectMapper.readValue(json.toString(),  envelopeType);
                        log.info("Incoming Payload: " + envelope.getPayload());
                        SocketIOJSONSubscriber.this.onMessage(envelope, envelope.getPayload());
                    }

                   
                } catch (Exception x) {
                    log.error("Threw exception while handling incoming message", x);
                }
            }
        });
        s.connect();
        return s;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        subscribe(defaultTopic);
    }
}