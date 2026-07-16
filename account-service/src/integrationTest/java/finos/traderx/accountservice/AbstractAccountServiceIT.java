package finos.traderx.accountservice;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Base for account-service integration tests: boots the real service on a random port
 * against a real in-memory H2 database seeded before each test from the canonical
 * {@code database/initialSchema.sql}, with a WireMock server standing in for the
 * {@code people-service} neighbour (driven off {@code people-service/openapi.yaml}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "file:../database/initialSchema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractAccountServiceIT {

    protected static final WireMockServer peopleService = new WireMockServer(options().dynamicPort());

    static {
        peopleService.start();
        configureFor("localhost", peopleService.port());
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("people.service.url", () -> "http://localhost:" + peopleService.port());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:accountsvc;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "sa");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.hibernate.naming.physical-strategy",
                () -> "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
    }

    @BeforeEach
    void resetStubs() {
        peopleService.resetAll();
    }

    @AfterEach
    void verifyNoUnmatched() {
        if (!peopleService.findAllUnmatchedRequests().isEmpty()) {
            throw new AssertionError("Unmatched requests to stubbed people-service: "
                    + peopleService.findAllUnmatchedRequests());
        }
    }
}
