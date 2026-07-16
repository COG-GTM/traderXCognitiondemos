package finos.traderx.tradeprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import finos.traderx.itsupport.EmbeddedTradeFeed;

/**
 * Base for trade-processor integration tests. Boots the real Spring context wired to an
 * in-JVM {@link EmbeddedTradeFeed} (real Socket.IO bus) and a real in-memory H2 database
 * created and seeded before every test from the canonical {@code database/initialSchema.sql}
 * so state is deterministic and isolated per test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "file:../database/initialSchema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractTradeProcessorIT {

    protected static final EmbeddedTradeFeed tradeFeed = new EmbeddedTradeFeed();

    static {
        tradeFeed.start();
    }

    @BeforeEach
    void resetFeedCapture() {
        tradeFeed.clearPublished();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("trade.feed.address", tradeFeed::getAddress);
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:tradeproc;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "sa");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.hibernate.naming.physical-strategy",
                () -> "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
    }
}
