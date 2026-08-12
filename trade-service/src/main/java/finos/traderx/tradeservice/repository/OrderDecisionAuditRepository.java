package finos.traderx.tradeservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import finos.traderx.tradeservice.model.audit.OrderDecisionAudit;

/**
 * Append-only access to the best-execution audit trail.
 *
 * This extends the bare {@link Repository} marker rather than {@code JpaRepository} on
 * purpose: inheriting {@code CrudRepository} would expose {@code delete}, {@code deleteAll}
 * and {@code saveAll}-style mutation to any caller in the service. Only the four methods
 * declared here exist.
 */
public interface OrderDecisionAuditRepository extends Repository<OrderDecisionAudit, String> {

    OrderDecisionAudit save(OrderDecisionAudit auditRecord);

    Optional<OrderDecisionAudit> findById(String id);

    /**
     * Ordered, because one correlation id can carry more than one record: an accepted order
     * that then failed to reach the trade feed is two rows, and the sequence is what makes the
     * pair readable. An unordered list would let a caller read the accept and miss the failure.
     * The secondary key breaks the tie when both rows fall in the same millisecond, which for
     * a synchronous publish failure is the normal case rather than the rare one.
     */
    List<OrderDecisionAudit> findByCorrelationIdOrderByDecisionTimestampAscRecordedAtAsc(String correlationId);

    List<OrderDecisionAudit> findByAccountIdOrderByDecisionTimestampAscRecordedAtAsc(Integer accountId);
}
