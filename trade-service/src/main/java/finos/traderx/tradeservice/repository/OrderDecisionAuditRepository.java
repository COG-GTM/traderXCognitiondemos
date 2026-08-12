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

    List<OrderDecisionAudit> findByCorrelationId(String correlationId);

    List<OrderDecisionAudit> findByAccountId(Integer accountId);
}
