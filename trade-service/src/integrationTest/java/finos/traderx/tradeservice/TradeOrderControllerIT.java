package finos.traderx.tradeservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import finos.traderx.itsupport.EmbeddedTradeFeed;
import finos.traderx.itsupport.TradeFeedTestClient;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

/**
 * S1: {@code trade-service} -> {@code trade-feed}. Submits a trade over the real REST
 * boundary and asserts it is published, exactly once, onto the Socket.IO {@code /trades}
 * topic with a payload matching the submitted order (asserted from the feed's server-side
 * capture, so the check does not depend on a second client receiving the re-broadcast).
 */
class TradeOrderControllerIT extends AbstractTradeServiceIT {

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
    void submittedTradeIsPublishedOnceToTradesTopic() {
        stubFor(get(urlEqualTo("/stocks/IBM"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"ticker\":\"IBM\",\"companyName\":\"International Business Machines\"}")));
        stubFor(get(urlEqualTo("/account/22214"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":22214,\"displayName\":\"Test Account 20\"}")));

        feedClient = new TradeFeedTestClient(tradeFeed.getAddress()).connectAndSubscribe(tradeFeed);
        // The service's publisher connects asynchronously on startup.
        await().atMost(Duration.ofSeconds(20)).until(tradePublisher::isConnected);

        ResponseEntity<String> response = postTrade(
                "{\"id\":\"ORD-1\",\"security\":\"IBM\",\"quantity\":100,\"accountId\":22214,\"side\":\"Buy\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(tradeFeed.publishedOn("/trades")).hasSize(1));

        JSONObject payload = tradeFeed.publishedOn("/trades").get(0).getJSONObject("payload");
        assertThat(payload.getString("security")).isEqualTo("IBM");
        assertThat(payload.getInt("accountId")).isEqualTo(22214);
        assertThat(payload.getInt("quantity")).isEqualTo(100);
        assertThat(payload.getString("side")).isEqualTo("Buy");
    }

    private ResponseEntity<String> postTrade(String body) {
        RequestEntity<String> request = RequestEntity
                .post(UriComponentsBuilder.fromPath("/trade/").build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        return rest.exchange(request, String.class);
    }
}
