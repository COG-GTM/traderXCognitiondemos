package finos.traderx.messaging.socketio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import io.socket.client.Socket;

class SocketIOJSONPublisherTests {

    private Socket mockSocket;
    private SocketIOJSONPublisher<TradeOrder> publisher;

    @BeforeEach
    void setUp() throws Exception {
        mockSocket = mock(Socket.class);
        publisher = new SocketIOJSONPublisher<TradeOrder>() {
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
        assertThrows(PubSubException.class, () -> publisher.publish(new TradeOrder()));
    }

    @Test
    void emitsEnvelopeWithoutNullFields() throws Exception {
        publisher.connected = true;

        publisher.publish(new TradeOrder("t-1", 1, "AAPL", TradeSide.Buy, 100));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(mockSocket).emit(eq("publish"), payload.capture());

        JSONObject envelope = (JSONObject) payload.getValue();
        assertEquals("/trades", envelope.getString("topic"));
        assertEquals("TradeOrder", envelope.getString("type"));
        assertFalse(envelope.has("from"), "null envelope fields must be omitted");

        JSONObject body = envelope.getJSONObject("payload");
        assertEquals("AAPL", body.getString("security"));
        assertEquals("Buy", body.getString("side"));
        assertEquals(100, body.getInt("quantity"));
        assertFalse(body.has("state"), "null payload fields must be omitted");
    }
}
