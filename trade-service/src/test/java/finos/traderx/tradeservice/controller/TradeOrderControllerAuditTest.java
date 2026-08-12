package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.audit.OrderDecisionAuditService;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.exceptions.ValidationUnavailableException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import finos.traderx.tradeservice.model.audit.DecisionOutcome;
import finos.traderx.tradeservice.model.audit.DecisionReason;

/**
 * Every exit path from the submission endpoint must leave exactly one audit record behind,
 * including the paths where a downstream lookup could not be performed at all.
 */
class TradeOrderControllerAuditTest {

    private static final String REFERENCE_DATA_URL = "http://reference-data";
    private static final String ACCOUNT_URL = "http://account-service";

    private OrderDecisionAuditService auditService;
    private Publisher<TradeOrder> publisher;
    private RestTemplate restTemplate;
    private MockRestServiceServer downstream;
    private TradeOrderController controller;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        auditService = mock(OrderDecisionAuditService.class);
        publisher = mock(Publisher.class);
        restTemplate = new RestTemplate();
        downstream = MockRestServiceServer.bindTo(restTemplate).build();
        controller = new TradeOrderController(publisher, auditService, restTemplate);
        ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", REFERENCE_DATA_URL);
        ReflectionTestUtils.setField(controller, "accountServiceAddress", ACCOUNT_URL);
    }

    private TradeOrder order() {
        return new TradeOrder("ORDER-1", 22214, "IBM", TradeSide.Buy, 100);
    }

    private DecisionReason capturedReason(DecisionOutcome expectedOutcome) {
        ArgumentCaptor<DecisionReason> reason = ArgumentCaptor.forClass(DecisionReason.class);
        verify(auditService).recordDecision(any(TradeOrder.class), anyString(), eq(expectedOutcome), reason.capture(),
                anyString());
        return reason.getValue();
    }

    @Test
    void anAcceptedOrderIsAuditedAndCarriesACorrelationId() throws Exception {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}", MediaType.APPLICATION_JSON));
        downstream.expect(requestTo(ACCOUNT_URL + "/account/22214"))
                .andRespond(withSuccess("{\"id\":22214,\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));

        TradeOrder submitted = controller.createTradeOrder(order(), "user01").getBody();

        assertNotNull(submitted.getCorrelationId());
        assertEquals(DecisionReason.VALIDATED, capturedReason(DecisionOutcome.ACCEPTED));
        verify(publisher).publish(eq("/trades"), any(TradeOrder.class));
    }

    @Test
    void anUnknownSecurityIsAuditedAsRejected() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(), "user01"));

        assertEquals(DecisionReason.SECURITY_NOT_FOUND, capturedReason(DecisionOutcome.REJECTED));
    }

    @Test
    void anUnknownAccountIsAuditedAsRejected() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}", MediaType.APPLICATION_JSON));
        downstream.expect(requestTo(ACCOUNT_URL + "/account/22214"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(), "user01"));

        assertEquals(DecisionReason.ACCOUNT_NOT_FOUND, capturedReason(DecisionOutcome.REJECTED));
    }

    @Test
    void anOrderRefusedBecauseAValidationServiceFailedIsStillAudited() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM")).andRespond(withServerError());

        assertThrows(ValidationUnavailableException.class, () -> controller.createTradeOrder(order(), "user01"));

        assertEquals(DecisionReason.VALIDATION_UNAVAILABLE, capturedReason(DecisionOutcome.REJECTED));
    }

    @Test
    void anOverlongSubmittingUserIsTruncatedToTheAuditColumnWidth() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class,
                () -> controller.createTradeOrder(order(), "u".repeat(200)));

        ArgumentCaptor<String> submittedBy = ArgumentCaptor.forClass(String.class);
        verify(auditService).recordDecision(any(TradeOrder.class), anyString(), any(DecisionOutcome.class),
                any(DecisionReason.class), submittedBy.capture());
        assertEquals(50, submittedBy.getValue().length());
    }

    @Test
    void anEmptyBodyFromALookupServiceDoesNotEscapeTheAuditPath() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withSuccess());
        downstream.expect(requestTo(ACCOUNT_URL + "/account/22214"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(), "user01"));

        assertEquals(DecisionReason.ACCOUNT_NOT_FOUND, capturedReason(DecisionOutcome.REJECTED));
    }

    @Test
    void anAnonymousSubmissionIsRecordedAsUnknownRatherThanNotAtAll() {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> controller.createTradeOrder(order(), null));

        ArgumentCaptor<String> submittedBy = ArgumentCaptor.forClass(String.class);
        verify(auditService).recordDecision(any(TradeOrder.class), anyString(), any(DecisionOutcome.class),
                any(DecisionReason.class), submittedBy.capture());
        assertEquals("UNKNOWN", submittedBy.getValue());
    }
}
