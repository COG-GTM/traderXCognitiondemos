package finos.traderx.positionservice.model.audit;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only projection of the best-execution audit record written by trade-service (TRX-104),
 * retained under MiFID II Art. 16(6) and read here by the compliance query API.
 *
 * position-service owns no part of this table's lifecycle: it never inserts, updates or deletes
 * a record. The mapping is therefore {@link Immutable}, declares every column
 * {@code updatable = false} and exposes getters only, so an accidental mutation from the query
 * side fails to compile rather than rewriting the record a regulator will later read.
 *
 * {@code reasonCode} is deliberately a String rather than an enum: trade-service may add a
 * reason code without position-service being redeployed, and a reader that throws on an
 * unrecognised value would hide exactly the decisions it is there to surface.
 */
@Entity
@Immutable
@Table(name = "ORDERDECISIONAUDIT")
public class OrderDecisionAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", length = 50, updatable = false, nullable = false)
    private String id;

    @Column(name = "CORRELATIONID", length = 50, updatable = false, nullable = false)
    private String correlationId;

    @Column(name = "ORDERID", length = 50, updatable = false)
    private String orderId;

    @Column(name = "ACCOUNTID", updatable = false)
    private Integer accountId;

    @Column(name = "SECURITY", length = 50, updatable = false)
    private String security;

    @Column(name = "SIDE", length = 10, updatable = false)
    private String side;

    @Column(name = "QUANTITY", updatable = false)
    private Integer quantity;

    @Column(name = "PRICE", precision = 19, scale = 4, updatable = false)
    private BigDecimal price;

    @Column(name = "PRICESOURCE", length = 40, updatable = false)
    private String priceSource;

    @Column(name = "NOTIONAL", precision = 23, scale = 4, updatable = false)
    private BigDecimal notional;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISION", length = 10, updatable = false, nullable = false)
    private DecisionOutcome decision;

    @Column(name = "REASONCODE", length = 40, updatable = false, nullable = false)
    private String reasonCode;

    @Column(name = "LIMITID", length = 40, updatable = false)
    private String limitId;

    @Column(name = "LIMITTYPE", length = 40, updatable = false)
    private String limitType;

    @Column(name = "LIMITVALUE", precision = 23, scale = 4, updatable = false)
    private BigDecimal limitValue;

    @Column(name = "LIMITEFFECTIVEFROM", updatable = false)
    private Instant limitEffectiveFrom;

    @Column(name = "SUBMITTEDBY", length = 50, updatable = false)
    private String submittedBy;

    @Column(name = "DECISIONTIMESTAMP", updatable = false, nullable = false)
    private Instant decisionTimestamp;

    protected OrderDecisionAudit() {
    }

    public String getId() {
        return id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getSecurity() {
        return security;
    }

    public String getSide() {
        return side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getPriceSource() {
        return priceSource;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public DecisionOutcome getDecision() {
        return decision;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getLimitId() {
        return limitId;
    }

    public String getLimitType() {
        return limitType;
    }

    public BigDecimal getLimitValue() {
        return limitValue;
    }

    public Instant getLimitEffectiveFrom() {
        return limitEffectiveFrom;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public Instant getDecisionTimestamp() {
        return decisionTimestamp;
    }

    @Override
    public String toString() {
        return "OrderDecisionAudit[id=%s, correlationId=%s, decision=%s, reason=%s, account=%s, security=%s]"
                .formatted(id, correlationId, decision, reasonCode, accountId, security);
    }
}
