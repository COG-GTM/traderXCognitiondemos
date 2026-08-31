package finos.traderx.tradeservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

@WebMvcTest(TradeOrderController.class)
class TradeOrderControllerTests {

    private static final String REFERENCE_DATA_URL = "http://reference-data:18085";
    private static final String ACCOUNT_SERVICE_URL = "http://account-service:18088";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TradeOrderController tradeOrderController;

    @MockitoBean
    Publisher<TradeOrder> tradePublisher;

    private MockRestServiceServer downstream;

    @BeforeEach
    void bindDownstreamServices() {
        RestTemplate restTemplate = new RestTemplate();
        downstream = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(tradeOrderController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(tradeOrderController, "referenceDataServiceAddress", REFERENCE_DATA_URL);
        ReflectionTestUtils.setField(tradeOrderController, "accountServiceAddress", ACCOUNT_SERVICE_URL);
    }

    private String order(String security, int accountId) {
        return "{\"id\":\"t-1\",\"security\":\"" + security + "\",\"accountId\":" + accountId
                + ",\"side\":\"Buy\",\"quantity\":100}";
    }

    @Test
    void publishesValidTradeOrder() throws Exception {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/AAPL"))
                .andRespond(withSuccess("{\"ticker\":\"AAPL\",\"name\":\"Apple\"}", MediaType.APPLICATION_JSON));
        downstream.expect(requestTo(ACCOUNT_SERVICE_URL + "/account/1"))
                .andRespond(withSuccess("{\"id\":1,\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/trade/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(order("AAPL", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security").value("AAPL"));

        verify(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));
        downstream.verify();
    }

    @Test
    void rejectsOrderWhenTickerIsUnknown() throws Exception {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/NOPE"))
                .andRespond(withResourceNotFound());

        mockMvc.perform(post("/trade/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(order("NOPE", 1)))
                .andExpect(status().isNotFound());

        verify(tradePublisher, never()).publish(any(), any());
        downstream.verify();
    }

    @Test
    void rejectsOrderWhenAccountIsUnknown() throws Exception {
        downstream.expect(requestTo(REFERENCE_DATA_URL + "/stocks/AAPL"))
                .andRespond(withSuccess("{\"ticker\":\"AAPL\",\"name\":\"Apple\"}", MediaType.APPLICATION_JSON));
        downstream.expect(requestTo(ACCOUNT_SERVICE_URL + "/account/99"))
                .andRespond(withResourceNotFound());

        mockMvc.perform(post("/trade/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(order("AAPL", 99)))
                .andExpect(status().isNotFound());

        verify(tradePublisher, never()).publish(any(), any());
        downstream.verify();
    }
}
