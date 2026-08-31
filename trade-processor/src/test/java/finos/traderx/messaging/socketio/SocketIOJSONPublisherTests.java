package finos.traderx.messaging.socketio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeprocessor.model.Trade;
import io.socket.client.Socket;

class SocketIOJSONPublisherTests {

    private Socket mockSocket;
    private SocketIOJSONPublisher<Trade> publisher;

    @BeforeEach
    void setUp() throws Exception {
        mockSocket = mock(Socket.class);
        publisher = new SocketIOJSONPublisher<Trade>() {
            @Override
            protected Socket internalConnect(URI uri) {
                return mockSocket;
            }
        };
        publisher.setTopic("/trades");
        publisher.afterPropertiesSet();
    }

    @Test
    void refusesToPublishWhileDisconnected() {
        assertThrows(PubSubException.class, () -> publisher.publish(new Trade()));
    }

    @Test
    void emitsEnvelopeWithoutNullFields() throws Exception {
        publisher.connected = true;

        Trade trade = new Trade();
        trade.setId("t-1");
        trade.setAccountId(1);
        trade.setSecurity("AAPL");
        trade.setQuantity(100);

        publisher.publish(trade);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(mockSocket).emit(eq("publish"), payload.capture());

        JSONObject envelope = (JSONObject) payload.getValue();
        assertEquals("/trades", envelope.getString("topic"));
        assertEquals("Trade", envelope.getString("type"));
        assertFalse(envelope.has("from"), "null envelope fields must be omitted");

        JSONObject body = envelope.getJSONObject("payload");
        assertEquals("AAPL", body.getString("security"));
        assertTrue(body.has("quantity"));
        assertFalse(body.has("side"), "null payload fields must be omitted");
    }

    @Test
    void connectRegistersLifecycleListeners() {
        verify(mockSocket).on(eq(Socket.EVENT_CONNECT), any());
        verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), any());
        verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), any());
        verify(mockSocket).connect();
    }
}
