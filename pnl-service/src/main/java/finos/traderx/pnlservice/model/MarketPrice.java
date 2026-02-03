package finos.traderx.pnlservice.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MARKETPRICES")
public class MarketPrice implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(length = 15, name = "SECURITY")
	private String security;

	public String getSecurity() {
		return this.security;
	}

	public void setSecurity(String security) {
		this.security = security;
	}

	@Column(name = "PRICE", precision = 10, scale = 2)
	private BigDecimal price;

	public BigDecimal getPrice() {
		return this.price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Column(name = "UPDATED")
	private Date updated;

	public void setUpdated(Date u) {
		this.updated = u;
	}

	public Date getUpdated() {
		return this.updated;
	}
}
