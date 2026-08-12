package finos.traderx.accountservice.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "RISKLIMITS")
public class RiskLimit implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "AccountID")
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

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", nullable = false)
	private Date updated;

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

	public Date getUpdated() {
		return this.updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}
}
