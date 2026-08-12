package finos.traderx.accountservice.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Append-only record of a risk limit value that was in force at some point in time.
 * Rows are inserted, never updated or deleted.
 */
@Entity
@Table(name = "RISKLIMITHISTORY")
public class RiskLimitHistory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "risk_limit_history_generator")
	@SequenceGenerator(name = "risk_limit_history_generator", sequenceName = "RISKLIMITHISTORY_SEQ", allocationSize = 1)
	private long id;

	@Column(name = "AccountID", nullable = false)
	private int accountId;

	@Column(name = "MaxOrderNotional", nullable = false, precision = 19, scale = 2)
	private BigDecimal maxOrderNotional;

	@Column(name = "Currency", length = 3, nullable = false)
	private String currency;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EffectiveFrom", nullable = false)
	private Date effectiveFrom;

	@Column(name = "SetBy", length = 50, nullable = false)
	private String setBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "ChangeType", length = 10, nullable = false)
	private RiskLimitChangeType changeType;

	@Column(name = "ChangedBy", length = 50, nullable = false)
	private String changedBy;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ChangedAt", nullable = false)
	private Date changedAt;

	@Column(name = "Reason", length = 255)
	private String reason;

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getAccountId() {
		return this.accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getMaxOrderNotional() {
		return this.maxOrderNotional;
	}

	public void setMaxOrderNotional(BigDecimal maxOrderNotional) {
		this.maxOrderNotional = maxOrderNotional;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Date getEffectiveFrom() {
		return this.effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public String getSetBy() {
		return this.setBy;
	}

	public void setSetBy(String setBy) {
		this.setBy = setBy;
	}

	public RiskLimitChangeType getChangeType() {
		return this.changeType;
	}

	public void setChangeType(RiskLimitChangeType changeType) {
		this.changeType = changeType;
	}

	public String getChangedBy() {
		return this.changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public Date getChangedAt() {
		return this.changedAt;
	}

	public void setChangedAt(Date changedAt) {
		this.changedAt = changedAt;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
