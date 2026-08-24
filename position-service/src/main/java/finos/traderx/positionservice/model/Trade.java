package finos.traderx.positionservice.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TRADES")
public class Trade implements Serializable {

	private static final long serialVersionUID = 1L;

    @Column(length = 100, name = "ID")
    @Id
	private String id;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
    
	@Column(name = "ACCOUNTID")
	private Integer accountId;

	public Integer getAccountId() {
		return this.accountId;
	}

	public void setAccountId(Integer id) {
		this.accountId = id;
	}



    @Column(length = 50, name = "SECURITY")
	private String security;

	public String getSecurity() {
		return this.security;
	}

	public void setSecurity(String security) {
		this.security = security;
	}

    @Column(length = 4, name = "SIDE")
	private String side;

	public String getSide() {
		return this.side;
	}

	public void setSide(String side) {
		this.side = side;
	}

    @Column(length = 20, name = "STATE")
	private String state="UNSET";

	public String getState() {
		return this.state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Column(name = "QUANTITY")
	private Integer quantity;

	public Integer getQuantity() {
		return this.quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	@Column(name = "UPDATED")
	private Date updated;

	public void setUpdated(Date u){
		this.updated=u;
	}

	public Date getUpdated(){
		return this.updated;
	}


	@Column(length = 52, name = "UTI")
	private String uti;

	public String getUti() {
		return this.uti;
	}

	public void setUti(String uti) {
		this.uti = uti;
	}

	@Column(length = 20, name = "REPORTINGCOUNTERPARTYLEI")
	private String reportingCounterpartyLei;

	public String getReportingCounterpartyLei() {
		return this.reportingCounterpartyLei;
	}

	public void setReportingCounterpartyLei(String reportingCounterpartyLei) {
		this.reportingCounterpartyLei = reportingCounterpartyLei;
	}

	@Column(length = 20, name = "REPORTINGREGIME")
	private String reportingRegime;

	public String getReportingRegime() {
		return this.reportingRegime;
	}

	public void setReportingRegime(String reportingRegime) {
		this.reportingRegime = reportingRegime;
	}

	@Column(name = "CREATED")
	private Date created;

	public void setCreated(Date u){
		this.created=u;
	}

	public Date getCreated(){
        return this.created;
	}
}
