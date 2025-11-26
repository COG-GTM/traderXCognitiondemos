package finos.traderx.messaging.socketio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

class SocketIOEnvelopeTest {

    @Test
    void constructor_WithTopicAndPayload_SetsAllFields() {
        String payload = "Test payload";
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/test-topic", payload);

        assertEquals("/test-topic", envelope.getTopic());
        assertEquals("Test payload", envelope.getPayload());
        assertEquals("String", envelope.getType());
        assertNotNull(envelope.getDate());
    }

    @Test
    void defaultConstructor_CreatesEmptyEnvelope() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();

        assertNull(envelope.getTopic());
        assertNull(envelope.getPayload());
        assertNull(envelope.getType());
        assertNull(envelope.getFrom());
        assertNotNull(envelope.getDate());
    }

    @Test
    void setAndGetTopic_WorksCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setTopic("/trades");

        assertEquals("/trades", envelope.getTopic());
    }

    @Test
    void setAndGetPayload_WorksCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setPayload("Test message");

        assertEquals("Test message", envelope.getPayload());
    }

    @Test
    void setAndGetType_WorksCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setType("CustomType");

        assertEquals("CustomType", envelope.getType());
    }

    @Test
    void setAndGetFrom_WorksCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setFrom("sender-123");

        assertEquals("sender-123", envelope.getFrom());
    }

    @Test
    void getDate_ReturnsNonNullDate() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/topic", "payload");

        assertNotNull(envelope.getDate());
        assertTrue(envelope.getDate() instanceof Date);
    }

    @Test
    void constructor_SetsTypeFromPayloadClass() {
        Integer intPayload = 42;
        SocketIOEnvelope<Integer> envelope = new SocketIOEnvelope<>("/numbers", intPayload);

        assertEquals("Integer", envelope.getType());
    }

    @Test
    void constructor_WithCustomObject_SetsTypeCorrectly() {
        TestPayload payload = new TestPayload("test");
        SocketIOEnvelope<TestPayload> envelope = new SocketIOEnvelope<>("/custom", payload);

        assertEquals("TestPayload", envelope.getType());
    }

    @Test
    void dateIsSetAtCreationTime() {
        Date before = new Date();
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/topic", "payload");
        Date after = new Date();

        assertTrue(envelope.getDate().getTime() >= before.getTime());
        assertTrue(envelope.getDate().getTime() <= after.getTime());
    }

    @Test
    void allFieldsCanBeSetAndRetrieved() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setTopic("/all-fields");
        envelope.setPayload("Complete payload");
        envelope.setType("CompleteType");
        envelope.setFrom("complete-sender");

        assertEquals("/all-fields", envelope.getTopic());
        assertEquals("Complete payload", envelope.getPayload());
        assertEquals("CompleteType", envelope.getType());
        assertEquals("complete-sender", envelope.getFrom());
    }

    private static class TestPayload {
        private String value;

        TestPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
