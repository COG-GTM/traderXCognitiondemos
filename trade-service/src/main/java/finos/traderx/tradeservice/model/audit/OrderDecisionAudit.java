package finos.traderx.tradeservice.model.audit;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only record of a single order decision, retained under MiFID II Art. 16(6).
 *
 * Immutability is enforced in three places: Hibernate never issues an UPDATE for an
 * {@link Immutable} entity, every column is mapped {@code updatable = false}, and the type
 * exposes no setters. The repository deliberately exposes no delete operation.
 */
@Entity
@Immutable
@Table(name = "ORDERDECISIONAUDIT")
public class OrderDecisionAudit implements Persistable<String>, Serializable {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "REASONCODE", length = 40, updatable = false, nullable = false)
    private DecisionReason reasonCode;

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

    public OrderDecisionAudit(String id, String correlationId, String orderId, Integer accountId, String security,
            String side, Integer quantity, BigDecimal price, String priceSource, BigDecimal notional,
            DecisionOutcome decision, DecisionReason reasonCode, EvaluatedLimit limit, String submittedBy,
            Instant decisionTimestamp) {
        this.id = id;
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.accountId = accountId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.priceSource = priceSource;
        this.notional = notional;
        this.decision = decision;
        this.reasonCode = reasonCode;
        if (limit != null) {
            this.limitId = limit.limitId();
            this.limitType = limit.limitType();
            this.limitValue = limit.limitValue();
            this.limitEffectiveFrom = limit.effectiveFrom();
        }
        this.submittedBy = submittedBy;
        this.decisionTimestamp = decisionTimestamp;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Always new. An audit record is inserted once and never merged back, so the persistence
     * provider must never be given the chance to turn a save into an UPDATE.
     */
    @Override
    public boolean isNew() {
        return true;
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

    public DecisionReason getReasonCode() {
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
        return "OrderDecisionAudit[id=%s, correlationId=%s, decision=%s, reason=%s, account=%s, security=%s, notional=%s]"
                .formatted(id, correlationId, decision, reasonCode, accountId, security, notional);
    }
}
