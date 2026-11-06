package finos.traderx.tradeprocessor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.service.TradeService;

@WebMvcTest
class TradeOrderEndpointRemovedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @Test
    void tradeOrdersCannotBeBookedOverHttp() throws Exception {
        String order = "{\"id\":\"x\",\"state\":\"New\",\"security\":\"FAKE\",\"quantity\":1000000,\"accountId\":22,\"side\":\"Sell\"}";

        mockMvc.perform(post("/tradeservice/order")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "http://evil.example")
                .content(order))
                .andExpect(status().isNotFound());

        verify(tradeService, never()).processTrade(any(TradeOrder.class));
    }
}
