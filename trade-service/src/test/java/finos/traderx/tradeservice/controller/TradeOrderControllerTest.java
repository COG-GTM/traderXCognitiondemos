package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerTest {

    @Mock
    private Publisher<TradeOrder> tradePublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TradeOrderController tradeOrderController;

    private static final String REFERENCE_DATA_SERVICE_URL = "http://localhost:18085";
    private static final String ACCOUNT_SERVICE_URL = "http://localhost:18088";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tradeOrderController, "referenceDataServiceAddress", REFERENCE_DATA_SERVICE_URL);
        ReflectionTestUtils.setField(tradeOrderController, "accountServiceAddress", ACCOUNT_SERVICE_URL);
        ReflectionTestUtils.setField(tradeOrderController, "restTemplate", restTemplate);
    }

    @Test
    void createTradeOrder_Success() throws PubSubException {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);
        Security security = new Security("AAPL", "Apple Inc.");
        Account account = new Account(100001, "Test Account");

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/AAPL"), 
                eq(Security.class)))
                .thenReturn(ResponseEntity.ok(security));
        
        when(restTemplate.getForEntity(
                eq(ACCOUNT_SERVICE_URL + "//account/100001"), 
                eq(Account.class)))
                .thenReturn(ResponseEntity.ok(account));

        doNothing().when(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));

        ResponseEntity<TradeOrder> response = tradeOrderController.createTradeOrder(tradeOrder);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AAPL", response.getBody().getSecurity());
        assertEquals(100001, response.getBody().getAccountId());
        assertEquals(TradeSide.Buy, response.getBody().getSide());
        assertEquals(100, response.getBody().getQuantity());

        verify(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));
    }

    @Test
    void createTradeOrder_InvalidTicker_ThrowsResourceNotFoundException() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "INVALID", TradeSide.Buy, 100);

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/INVALID"), 
                eq(Security.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, 
                        "Not Found", 
                        null, 
                        null, 
                        null));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tradeOrderController.createTradeOrder(tradeOrder));

        assertTrue(exception.getMessage().contains("INVALID"));
        assertTrue(exception.getMessage().contains("not found in Reference data service"));
    }

    @Test
    void createTradeOrder_InvalidAccount_ThrowsResourceNotFoundException() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 999999, "AAPL", TradeSide.Buy, 100);
        Security security = new Security("AAPL", "Apple Inc.");

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/AAPL"), 
                eq(Security.class)))
                .thenReturn(ResponseEntity.ok(security));

        when(restTemplate.getForEntity(
                eq(ACCOUNT_SERVICE_URL + "//account/999999"), 
                eq(Account.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, 
                        "Not Found", 
                        null, 
                        null, 
                        null));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tradeOrderController.createTradeOrder(tradeOrder));

        assertTrue(exception.getMessage().contains("999999"));
        assertTrue(exception.getMessage().contains("not found in Account service"));
    }

    @Test
    void createTradeOrder_PublisherFails_ThrowsRuntimeException() throws PubSubException {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);
        Security security = new Security("AAPL", "Apple Inc.");
        Account account = new Account(100001, "Test Account");

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/AAPL"), 
                eq(Security.class)))
                .thenReturn(ResponseEntity.ok(security));
        
        when(restTemplate.getForEntity(
                eq(ACCOUNT_SERVICE_URL + "//account/100001"), 
                eq(Account.class)))
                .thenReturn(ResponseEntity.ok(account));

        doThrow(new PubSubException("Connection failed"))
                .when(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tradeOrderController.createTradeOrder(tradeOrder));

        assertTrue(exception.getMessage().contains("Failed to publish trade order"));
    }

    @Test
    void createTradeOrder_SellOrder_Success() throws PubSubException {
        TradeOrder tradeOrder = new TradeOrder("trade-456", 100002, "MSFT", TradeSide.Sell, 50);
        Security security = new Security("MSFT", "Microsoft Corporation");
        Account account = new Account(100002, "Another Account");

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/MSFT"), 
                eq(Security.class)))
                .thenReturn(ResponseEntity.ok(security));
        
        when(restTemplate.getForEntity(
                eq(ACCOUNT_SERVICE_URL + "//account/100002"), 
                eq(Account.class)))
                .thenReturn(ResponseEntity.ok(account));

        doNothing().when(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));

        ResponseEntity<TradeOrder> response = tradeOrderController.createTradeOrder(tradeOrder);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TradeSide.Sell, response.getBody().getSide());
        assertEquals(50, response.getBody().getQuantity());
    }

    @Test
    void createTradeOrder_ReferenceDataServiceError_ReturnsFalse() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/AAPL"), 
                eq(Security.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Internal Server Error", 
                        null, 
                        null, 
                        null));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tradeOrderController.createTradeOrder(tradeOrder));

        assertTrue(exception.getMessage().contains("AAPL"));
    }

    @Test
    void createTradeOrder_AccountServiceError_ReturnsFalse() {
        TradeOrder tradeOrder = new TradeOrder("trade-123", 100001, "AAPL", TradeSide.Buy, 100);
        Security security = new Security("AAPL", "Apple Inc.");

        when(restTemplate.getForEntity(
                eq(REFERENCE_DATA_SERVICE_URL + "//stocks/AAPL"), 
                eq(Security.class)))
                .thenReturn(ResponseEntity.ok(security));

        when(restTemplate.getForEntity(
                eq(ACCOUNT_SERVICE_URL + "//account/100001"), 
                eq(Account.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Internal Server Error", 
                        null, 
                        null, 
                        null));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tradeOrderController.createTradeOrder(tradeOrder));

        assertTrue(exception.getMessage().contains("100001"));
    }
}
