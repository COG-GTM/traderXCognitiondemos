package finos.traderx.messaging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PubSubExceptionTest {

    @Test
    void constructor_WithMessage_SetsMessage() {
        String message = "Connection failed";
        PubSubException exception = new PubSubException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void constructor_WithMessageAndThrowable_SetsBoth() {
        String message = "Connection failed";
        Throwable cause = new RuntimeException("Root cause");
        PubSubException exception = new PubSubException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructor_WithThrowable_SetsCause() {
        Throwable cause = new RuntimeException("Root cause");
        PubSubException exception = new PubSubException(cause);

        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("Root cause"));
    }

    @Test
    void extendsException() {
        PubSubException exception = new PubSubException("Test");

        assertTrue(exception instanceof Exception);
    }

    @Test
    void canBeThrown() {
        assertThrows(PubSubException.class, () -> {
            throw new PubSubException("Test exception");
        });
    }

    @Test
    void canBeCaughtAsException() {
        String message = "Caught exception message";
        try {
            throw new PubSubException(message);
        } catch (Exception e) {
            assertEquals(message, e.getMessage());
            assertTrue(e instanceof PubSubException);
        }
    }

    @Test
    void constructor_WithDifferentMessages_SetsCorrectMessage() {
        PubSubException exception1 = new PubSubException("Error 1");
        PubSubException exception2 = new PubSubException("Error 2");

        assertEquals("Error 1", exception1.getMessage());
        assertEquals("Error 2", exception2.getMessage());
    }

    @Test
    void constructor_WithNestedCause_PreservesCauseChain() {
        Throwable rootCause = new IllegalArgumentException("Root");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        PubSubException exception = new PubSubException("Top level", intermediateCause);

        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }
}
