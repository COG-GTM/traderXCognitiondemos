package finos.traderx.messaging.socketio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SocketIOEnvelopeTest {

    @Test
    void constructsWithTopicAndPayload() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/trades", "test-payload");

        assertEquals("/trades", envelope.getTopic());
        assertEquals("test-payload", envelope.getPayload());
        assertEquals("String", envelope.getType());
        assertNotNull(envelope.getDate());
    }

    @Test
    void defaultConstructorCreatesEmptyEnvelope() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();

        assertNull(envelope.getTopic());
        assertNull(envelope.getPayload());
        assertNull(envelope.getType());
        assertNull(envelope.getFrom());
        assertNotNull(envelope.getDate());
    }

    @Test
    void settersUpdateFields() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();

        envelope.setTopic("/positions");
        envelope.setPayload("data");
        envelope.setType("String");
        envelope.setFrom("trade-processor");

        assertEquals("/positions", envelope.getTopic());
        assertEquals("data", envelope.getPayload());
        assertEquals("String", envelope.getType());
        assertEquals("trade-processor", envelope.getFrom());
    }

    @Test
    void typeIsDerivedFromPayloadClassName() {
        SocketIOEnvelope<Integer> envelope = new SocketIOEnvelope<>("/test", 42);
        assertEquals("Integer", envelope.getType());
    }

    @Test
    void implementsEnvelopeInterface() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/test", "data");
        assertInstanceOf(finos.traderx.messaging.Envelope.class, envelope);
    }
}
