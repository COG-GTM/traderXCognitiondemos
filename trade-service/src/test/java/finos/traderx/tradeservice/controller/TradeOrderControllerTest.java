package finos.traderx.tradeservice.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

@WebMvcTest(TradeOrderController.class)
@TestPropertySource(properties = {
		"reference.data.service.url=http://reference-data",
		"account.service.url=http://account-service" })
class TradeOrderControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	TradeOrderController controller;

	@MockBean
	Publisher<TradeOrder> tradePublisher;

	MockRestServiceServer upstream;

	@BeforeEach
	void bindUpstream() {
		upstream = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate")).build();
	}

	private void expectRejected(String body) throws Exception {
		mockMvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
		verify(tradePublisher, never()).publish(anyString(), any());
		upstream.verify();
	}

	@Test
	void rejectsNegativeQuantity() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":-2147483000}");
	}

	@Test
	void rejectsMinusOneQuantity() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Sell\",\"quantity\":-1}");
	}

	@Test
	void rejectsZeroQuantity() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":0}");
	}

	@Test
	void rejectsNullQuantity() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\"}");
	}

	@Test
	void rejectsNullSide() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"quantity\":100}");
	}

	@Test
	void rejectsUnknownSide() throws Exception {
		expectRejected("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Short\",\"quantity\":100}");
	}

	@Test
	void publishesValidOrder() throws Exception {
		upstream.expect(requestTo(endsWith("/stocks/MSFT"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		upstream.expect(requestTo(endsWith("/account/22"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/trade/").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22,\"security\":\"MSFT\",\"side\":\"Buy\",\"quantity\":100}"))
				.andExpect(status().isOk());

		verify(tradePublisher).publish(eq("/trades"), any(TradeOrder.class));
		upstream.verify();
	}
}
