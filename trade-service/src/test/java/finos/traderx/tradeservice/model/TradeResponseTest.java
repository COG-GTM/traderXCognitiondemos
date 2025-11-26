package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TradeResponseTest {

    @Test
    void success_CreatesSuccessfulResponse() {
        TradeResponse response = TradeResponse.success("trade-123");

        assertTrue(response.isSuccess());
        assertEquals("trade-123", response.getId());
        assertNull(response.getErrorMessage());
    }

    @Test
    void error_CreatesErrorResponse() {
        TradeResponse response = TradeResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertNull(response.getId());
        assertEquals("Something went wrong", response.getErrorMessage());
    }

    @Test
    void setAndGetId_WorksCorrectly() {
        TradeResponse response = new TradeResponse();
        response.setId("unique-id-456");

        assertEquals("unique-id-456", response.getId());
    }

    @Test
    void setAndGetSuccess_True_WorksCorrectly() {
        TradeResponse response = new TradeResponse();
        response.setSuccess(true);

        assertTrue(response.isSuccess());
    }

    @Test
    void setAndGetSuccess_False_WorksCorrectly() {
        TradeResponse response = new TradeResponse();
        response.setSuccess(false);

        assertFalse(response.isSuccess());
    }

    @Test
    void setAndGetErrorMessage_WorksCorrectly() {
        TradeResponse response = new TradeResponse();
        response.setErrorMessage("Error occurred");

        assertEquals("Error occurred", response.getErrorMessage());
    }

    @Test
    void defaultValues_AreNullOrFalse() {
        TradeResponse response = new TradeResponse();

        assertFalse(response.isSuccess());
        assertNull(response.getId());
        assertNull(response.getErrorMessage());
    }

    @Test
    void success_WithDifferentIds_CreatesCorrectResponses() {
        TradeResponse response1 = TradeResponse.success("id-1");
        TradeResponse response2 = TradeResponse.success("id-2");

        assertEquals("id-1", response1.getId());
        assertEquals("id-2", response2.getId());
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
    }

    @Test
    void error_WithDifferentMessages_CreatesCorrectResponses() {
        TradeResponse response1 = TradeResponse.error("Error 1");
        TradeResponse response2 = TradeResponse.error("Error 2");

        assertEquals("Error 1", response1.getErrorMessage());
        assertEquals("Error 2", response2.getErrorMessage());
        assertFalse(response1.isSuccess());
        assertFalse(response2.isSuccess());
    }
}
