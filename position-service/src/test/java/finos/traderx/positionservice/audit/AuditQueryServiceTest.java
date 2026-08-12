package finos.traderx.positionservice.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import finos.traderx.positionservice.model.audit.AuditPage;
import finos.traderx.positionservice.model.audit.DecisionOutcome;
import finos.traderx.positionservice.model.audit.OrderDecisionView;
import finos.traderx.positionservice.repository.OrderDecisionAuditRepository;
import finos.traderx.positionservice.service.AuditQueryService;

@DataJpaTest
@TestPropertySource(properties = { "spring.jpa.hibernate.ddl-auto=create-drop" })
class AuditQueryServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private OrderDecisionAuditRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuditQueryService service;

    private AuditQueryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuditQueryProperties();
        service = new AuditQueryService(repository, properties);
        jdbcTemplate.update("DELETE FROM ORDERDECISIONAUDIT");

        insert("a1", 11413, "AAPL", DecisionOutcome.ACCEPTED, "VALIDATED", T0);
        insert("a2", 11413, "MSFT", DecisionOutcome.REJECTED, "ACCOUNT_LIMIT_BREACHED", T0.plusSeconds(60));
        insert("a3", 42422, "AAPL", DecisionOutcome.REJECTED, "SECURITY_NOT_FOUND", T0.plusSeconds(120));
        insert("a4", 42422, "AAPL", DecisionOutcome.ACCEPTED, "VALIDATED", T0.plusSeconds(180));
    }

    @Test
    void returnsEverythingNewestFirstWhenNoFilterIsGiven() {
        assertEquals(List.of("a4", "a3", "a2", "a1"), ids(service.search(null, null, null, null, null, null, null)));
    }

    @Test
    void filtersByAccountSecurityAndDecision() {
        assertEquals(List.of("a2", "a1"), ids(service.search(11413, null, null, null, null, null, null)));
        assertEquals(List.of("a4", "a3", "a1"), ids(service.search(null, "AAPL", null, null, null, null, null)));
        assertEquals(List.of("a3", "a2"),
                ids(service.search(null, null, DecisionOutcome.REJECTED, null, null, null, null)));
        assertEquals(List.of("a3"),
                ids(service.search(42422, "AAPL", DecisionOutcome.REJECTED, null, null, null, null)));
    }

    @Test
    void timeRangeIsInclusiveOfFromAndExclusiveOfTo() {
        AuditPage page = service.search(null, null, null, T0.plusSeconds(60), T0.plusSeconds(180), null, null);
        assertEquals(List.of("a3", "a2"), ids(page));
    }

    @Test
    void pagesWithoutLosingOrDuplicatingRecords() {
        AuditPage first = service.search(null, null, null, null, null, 0, 3);
        AuditPage second = service.search(null, null, null, null, null, 1, 3);

        assertEquals(List.of("a4", "a3", "a2"), ids(first));
        assertEquals(List.of("a1"), ids(second));
        assertEquals(4, first.totalElements());
        assertEquals(2, first.totalPages());
    }

    @Test
    void clampsRequestedPageSizeToTheConfiguredCeiling() {
        properties.setMaxPageSize(2);

        AuditPage page = service.search(null, null, null, null, null, 0, 1000);

        assertEquals(2, page.size());
        assertEquals(List.of("a4", "a3"), ids(page));
    }

    @Test
    void rejectsNonsensicalPagingAndRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> service.search(null, null, null, T0.plusSeconds(180), T0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, null, null, null, -1, null));
        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, null, null, null, 0, 0));
    }

    @Test
    void emptyResultIsAnEmptyPageRatherThanAnError() {
        AuditPage page = service.search(99999, null, null, null, null, null, null);

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
    }

    @Test
    void carriesTheLimitSnapshotThatWasInForceAtDecisionTime() {
        OrderDecisionView view = service.search(null, null, DecisionOutcome.REJECTED, null, null, null, null).content()
                .get(0);

        assertEquals("DEFAULT-ACCOUNT-NOTIONAL", view.limitId());
        assertEquals(new BigDecimal("5000000.0000"), view.limitValue());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), view.limitEffectiveFrom());
    }

    private List<String> ids(AuditPage page) {
        return page.content().stream().map(OrderDecisionView::id).toList();
    }

    private void insert(String id, Integer accountId, String security, DecisionOutcome decision, String reasonCode,
            Instant at) {
        jdbcTemplate.update("""
                INSERT INTO ORDERDECISIONAUDIT (ID, CORRELATIONID, ORDERID, ACCOUNTID, SECURITY, SIDE, QUANTITY,
                    PRICE, PRICESOURCE, NOTIONAL, DECISION, REASONCODE, LIMITID, LIMITTYPE, LIMITVALUE,
                    LIMITEFFECTIVEFROM, SUBMITTEDBY, DECISIONTIMESTAMP)
                VALUES (?, ?, ?, ?, ?, 'Buy', 100, 100.0000, 'CONFIGURED_STATIC_REFERENCE_PRICE', 10000.0000,
                    ?, ?, 'DEFAULT-ACCOUNT-NOTIONAL', 'ACCOUNT_NOTIONAL', 5000000.0000, ?, 'trader1', ?)
                """,
                id, "corr-" + id, "order-" + id, accountId, security, decision.name(), reasonCode,
                OffsetDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(at, ZoneOffset.UTC));
    }
}
