package finos.traderx.tradeservice;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;

import finos.traderx.itsupport.EmbeddedTradeFeed;

/**
 * Base for trade-service integration tests: boots the real Spring Boot service on a
 * random port, points it at an in-JVM {@link EmbeddedTradeFeed} (real Socket.IO bus)
 * and at a WireMock server standing in for the {@code reference-data} and
 * {@code account-service} neighbours. Both stubs are driven off the neighbours' REST
 * contracts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractTradeServiceIT {

    protected static final EmbeddedTradeFeed tradeFeed = new EmbeddedTradeFeed();
    protected static final WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    static {
        tradeFeed.start();
        wireMock.start();
        configureFor("localhost", wireMock.port());
    }

    @DynamicPropertySource
    static void serviceProperties(DynamicPropertyRegistry registry) {
        registry.add("trade.feed.address", tradeFeed::getAddress);
        registry.add("reference.data.service.url", () -> "http://localhost:" + wireMock.port());
        registry.add("account.service.url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
        tradeFeed.clearPublished();
    }

    @AfterEach
    void verifyNoUnmatched() {
        // Fail loudly if the service called a path no stub matched (contract drift).
        if (!wireMock.findAllUnmatchedRequests().isEmpty()) {
            throw new AssertionError("Unmatched requests to stubbed neighbours: "
                    + wireMock.findAllUnmatchedRequests());
        }
    }
}
