package finos.traderx.tradeservice.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_SetsMessage() {
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void constructor_WithDifferentMessages_SetsCorrectMessage() {
        ResourceNotFoundException exception1 = new ResourceNotFoundException("Account not found");
        ResourceNotFoundException exception2 = new ResourceNotFoundException("Security not found");

        assertEquals("Account not found", exception1.getMessage());
        assertEquals("Security not found", exception2.getMessage());
    }

    @Test
    void extendsRuntimeException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Test");

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void hasResponseStatusAnnotation() {
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(annotation);
    }

    @Test
    void responseStatusIsNotFound() {
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertEquals(HttpStatus.NOT_FOUND, annotation.value());
    }

    @Test
    void canBeThrown() {
        assertThrows(ResourceNotFoundException.class, () -> {
            throw new ResourceNotFoundException("Test exception");
        });
    }

    @Test
    void canBeCaught() {
        String message = "Caught exception message";
        try {
            throw new ResourceNotFoundException(message);
        } catch (ResourceNotFoundException e) {
            assertEquals(message, e.getMessage());
        }
    }
}
