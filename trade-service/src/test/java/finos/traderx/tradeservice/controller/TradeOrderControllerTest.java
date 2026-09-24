package finos.traderx.tradeservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

class TradeOrderControllerTest {

	private static final String REFERENCE_DATA_URL = "http://reference-data:18085";

	private static final String ACCOUNT_URL = "http://account-service:18088";

	private static final TradeOrder ORDER = new TradeOrder("trade-1", 1, "IBM", TradeSide.Buy, 10);

	private RecordingPublisher publisher;

	private MockRestServiceServer server;

	private TradeOrderController controller;

	@BeforeEach
	void setUp() {
		this.publisher = new RecordingPublisher();
		RestTemplate restTemplate = new RestTemplate();
		this.server = MockRestServiceServer.bindTo(restTemplate).build();
		RestTemplateBuilder builder = new RestTemplateBuilder() {
			@Override
			public RestTemplate build() {
				return restTemplate;
			}
		};
		this.controller = new TradeOrderController(this.publisher, builder, REFERENCE_DATA_URL, ACCOUNT_URL);
	}

	@Test
	void publishesValidTradeOrder() {
		expectTickerLookup(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM Corp\"}", MediaType.APPLICATION_JSON));
		expectAccountLookup(withSuccess("{\"id\":1,\"displayName\":\"Test Account\"}", MediaType.APPLICATION_JSON));

		ResponseEntity<TradeOrder> response = this.controller.createTradeOrder(ORDER);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(ORDER);
		assertThat(this.publisher.published).containsExactly(ORDER);
		this.server.verify();
	}

	@Test
	void rejectsUnknownTicker() {
		expectTickerLookup(withResourceNotFound());

		assertThatThrownBy(() -> this.controller.createTradeOrder(ORDER))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("IBM not found in Reference data service.");

		assertThat(this.publisher.published).isEmpty();
		this.server.verify();
	}

	@Test
	void rejectsUnknownAccount() {
		expectTickerLookup(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM Corp\"}", MediaType.APPLICATION_JSON));
		expectAccountLookup(withResourceNotFound());

		assertThatThrownBy(() -> this.controller.createTradeOrder(ORDER))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("1 not found in Account service.");

		assertThat(this.publisher.published).isEmpty();
		this.server.verify();
	}

	@Test
	void propagatesReferenceDataServerError() {
		expectTickerLookup(withServerError());

		assertThatThrownBy(() -> this.controller.createTradeOrder(ORDER))
				.isInstanceOf(HttpServerErrorException.class);

		assertThat(this.publisher.published).isEmpty();
	}

	@Test
	void wrapsPublishFailure() {
		expectTickerLookup(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM Corp\"}", MediaType.APPLICATION_JSON));
		expectAccountLookup(withSuccess("{\"id\":1,\"displayName\":\"Test Account\"}", MediaType.APPLICATION_JSON));
		this.publisher.failOnPublish = true;

		assertThatThrownBy(() -> this.controller.createTradeOrder(ORDER))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Failed to publish trade order")
				.hasCauseInstanceOf(PubSubException.class);
	}

	private void expectTickerLookup(org.springframework.test.web.client.ResponseCreator response) {
		this.server.expect(requestTo(REFERENCE_DATA_URL + "/stocks/IBM")).andRespond(response);
	}

	private void expectAccountLookup(org.springframework.test.web.client.ResponseCreator response) {
		this.server.expect(requestTo(ACCOUNT_URL + "/account/1")).andRespond(response);
	}

	private static final class RecordingPublisher implements Publisher<TradeOrder> {

		private final List<TradeOrder> published = new ArrayList<>();

		private boolean failOnPublish;

		@Override
		public void publish(TradeOrder message) throws PubSubException {
			publish("/trades", message);
		}

		@Override
		public void publish(String topic, TradeOrder message) throws PubSubException {
			if (this.failOnPublish) {
				throw new PubSubException("boom");
			}
			this.published.add(message);
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void connect() throws PubSubException {
		}

		@Override
		public void disconnect() throws PubSubException {
		}
	}
}
