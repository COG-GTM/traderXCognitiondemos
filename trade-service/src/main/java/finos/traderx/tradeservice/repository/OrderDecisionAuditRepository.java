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
     *
     * Ordered by the append key alone rather than by decision time: the two rows are written
     * in one request and usually share a millisecond, and the wall clock they come from can
     * step backwards under an NTP correction, which would invert the pair. RecordedAt cannot.
     */
    List<OrderDecisionAudit> findByCorrelationIdOrderByRecordedAtAsc(String correlationId);

    /** By decision time, which is the question an account-level enquiry asks, then by append order. */
    List<OrderDecisionAudit> findByAccountIdOrderByDecisionTimestampAscRecordedAtAsc(Integer accountId);
}
