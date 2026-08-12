package finos.traderx.positionservice.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import finos.traderx.positionservice.model.audit.DecisionOutcome;
import finos.traderx.positionservice.model.audit.OrderDecisionAudit;

/**
 * Read-only access to the best-execution audit trail.
 *
 * Extends the bare {@link Repository} marker rather than {@code JpaRepository} on purpose, as
 * TRX-104's writer-side repository does: inheriting {@code CrudRepository} would hand every
 * caller {@code delete}, {@code deleteAll} and {@code save}, and this service has no business
 * writing to the record at all. {@code JpaSpecificationExecutor} is avoided for the same reason
 * - it carries {@code delete(Specification)} - so the filters are expressed as one JPQL query
 * with optional parameters instead.
 *
 * The ordering is {@code decisionTimestamp desc, id desc} rather than timestamp alone: two
 * decisions can land inside the same millisecond, and a page boundary that falls between them
 * must not be able to drop or duplicate a record.
 *
 * The security filter is compared case-insensitively. A reviewer who types a ticker in the wrong
 * case must not be told there are no decisions for it, and an empty page is indistinguishable
 * from a genuine absence. The caller is expected to upper-case the parameter; the {@code upper()}
 * on the column costs {@code IDX_OrderDecisionAudit_Security_Time} on that predicate, which is
 * the right trade for a filter whose wrong answer is silent.
 */
public interface OrderDecisionAuditRepository extends Repository<OrderDecisionAudit, String> {

    String FILTER = """
            from OrderDecisionAudit a
            where (:accountId is null or a.accountId = :accountId)
              and (:security is null or upper(a.security) = :security)
              and (:decision is null or a.decision = :decision)
              and (:from is null or a.decisionTimestamp >= :from)
              and (:to is null or a.decisionTimestamp < :to)
            """;

    @Query(value = "select a " + FILTER + " order by a.decisionTimestamp desc, a.id desc",
            countQuery = "select count(a) " + FILTER)
    Page<OrderDecisionAudit> search(@Param("accountId") Integer accountId,
            @Param("security") String security,
            @Param("decision") DecisionOutcome decision,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
