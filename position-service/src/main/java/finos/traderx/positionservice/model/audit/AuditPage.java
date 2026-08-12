package finos.traderx.positionservice.model.audit;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Paged envelope for audit queries. Spring's own {@code Page} serialisation is deliberately not
 * exposed on the wire: its JSON is unstable across Spring Data versions, and this response is
 * read by the blotter and, in time, by regulatory exports.
 */
public record AuditPage(List<OrderDecisionView> content, int page, int size, long totalElements, int totalPages) {

    public static AuditPage of(Page<OrderDecisionAudit> page) {
        return new AuditPage(
                page.getContent().stream().map(OrderDecisionView::of).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
