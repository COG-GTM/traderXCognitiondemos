package finos.traderx.messaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PubSubExceptionTest {

    @Test
    void constructsWithMessage() {
        PubSubException ex = new PubSubException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void constructsWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        PubSubException ex = new PubSubException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void constructsWithCauseOnly() {
        RuntimeException cause = new RuntimeException("root cause");
        PubSubException ex = new PubSubException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void isCheckedException() {
        assertInstanceOf(Exception.class, new PubSubException("test"));
        assertFalse(RuntimeException.class.isAssignableFrom(PubSubException.class));
    }
}
