package finos.traderx.tradeservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import finos.traderx.itsupport.TradeFeedTestClient;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

/**
 * S4/S5: {@code trade-service} -> {@code account-service} / {@code reference-data}.
 * Boots the service against WireMock neighbours and pins the fail-closed validation
 * contract: a missing ticker/account returns 404, an upstream 5xx returns 500, and in
 * every failure case <b>nothing</b> is published to the trade-feed.
 */
class TradeOrderValidationIT extends AbstractTradeServiceIT {

    private static final String VALID_ORDER =
            "{\"id\":\"ORD-1\",\"security\":\"IBM\",\"quantity\":100,\"accountId\":22214,\"side\":\"Buy\"}";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Publisher<TradeOrder> tradePublisher;

    private TradeFeedTestClient feedClient;

    @AfterEach
    void closeClient() {
        if (feedClient != null) {
            feedClient.close();
        }
    }

    @Test
    void unknownTickerReturns404AndPublishesNothing() {
        stubFor(get(urlEqualTo("/stocks/ZZZZ")).willReturn(aResponse().withStatus(404)));

        ResponseEntity<String> response = postValidatingNoPublish(
                "{\"id\":\"ORD-1\",\"security\":\"ZZZZ\",\"quantity\":100,\"accountId\":22214,\"side\":\"Buy\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(getRequestedFor(urlEqualTo("/stocks/ZZZZ")));
        assertNothingPublished();
    }

    @Test
    void unknownAccountReturns404AndPublishesNothing() {
        stubFor(get(urlEqualTo("/stocks/IBM"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}")));
        stubFor(get(urlEqualTo("/account/22214")).willReturn(aResponse().withStatus(404)));

        ResponseEntity<String> response = postValidatingNoPublish(VALID_ORDER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(getRequestedFor(urlEqualTo("/stocks/IBM")));
        verify(getRequestedFor(urlEqualTo("/account/22214")));
        assertNothingPublished();
    }

    @Test
    void referenceDataServerErrorReturns500AndPublishesNothing() {
        stubFor(get(urlEqualTo("/stocks/IBM")).willReturn(aResponse().withStatus(503)));

        ResponseEntity<String> response = postValidatingNoPublish(VALID_ORDER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNothingPublished();
    }

    @Test
    void accountServerErrorReturns500AndPublishesNothing() {
        stubFor(get(urlEqualTo("/stocks/IBM"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}")));
        stubFor(get(urlEqualTo("/account/22214")).willReturn(aResponse().withStatus(500)));

        ResponseEntity<String> response = postValidatingNoPublish(VALID_ORDER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNothingPublished();
    }

    private ResponseEntity<String> postValidatingNoPublish(String body) {
        feedClient = new TradeFeedTestClient(tradeFeed.getAddress())
                .connectAndSubscribe(tradeFeed, "/trades");
        await().atMost(Duration.ofSeconds(10)).until(tradePublisher::isConnected);
        RequestEntity<String> request = RequestEntity
                .post(UriComponentsBuilder.fromPath("/trade/").build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        return rest.exchange(request, String.class);
    }

    private void assertNothingPublished() {
        // Give any (erroneous) message time to arrive before asserting none did.
        await().pollDelay(Duration.ofMillis(750)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(feedClient.receivedOn("/trades")).isEmpty());
    }
}
