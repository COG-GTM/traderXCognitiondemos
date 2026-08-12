package finos.traderx.positionservice.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import finos.traderx.positionservice.audit.AuditQueryProperties;
import finos.traderx.positionservice.model.audit.AuditPage;
import finos.traderx.positionservice.model.audit.DecisionOutcome;
import finos.traderx.positionservice.model.audit.OrderDecisionAudit;
import finos.traderx.positionservice.repository.OrderDecisionAuditRepository;

/**
 * Query side of the best-execution audit trail. Read-only by construction: it holds a
 * repository that declares no mutating method.
 */
@Service
public class AuditQueryService {

    private final OrderDecisionAuditRepository auditRepository;

    private final AuditQueryProperties properties;

    public AuditQueryService(OrderDecisionAuditRepository auditRepository, AuditQueryProperties properties) {
        this.auditRepository = auditRepository;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * @param from inclusive lower bound, @param to exclusive upper bound. Half open so that
     *             adjacent windows requested by a reviewer neither overlap nor drop a decision
     *             that landed exactly on the boundary.
     */
    public AuditPage search(Integer accountId, String security, DecisionOutcome decision, Instant from, Instant to,
            Integer page, Integer size) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        Pageable pageable = pageRequest(page, size);
        Page<OrderDecisionAudit> results = auditRepository.search(accountId, blankToNull(security), decision, from, to,
                pageable);
        return AuditPage.of(results);
    }

    /**
     * Unsorted on purpose: the ordering lives in the query so it is applied identically to the
     * page and to the keys the indexes are built on.
     */
    private Pageable pageRequest(Integer page, Integer size) {
        int requestedPage = page == null ? 0 : page;
        if (requestedPage < 0) {
            throw new IllegalArgumentException("'page' must not be negative");
        }
        int requestedSize = size == null ? properties.getDefaultPageSize() : size;
        if (requestedSize < 1) {
            throw new IllegalArgumentException("'size' must be at least 1");
        }
        return PageRequest.of(requestedPage, Math.min(requestedSize, properties.getMaxPageSize()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
