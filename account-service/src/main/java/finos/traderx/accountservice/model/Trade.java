package finos.traderx.accountservice.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view of a persisted trade, used by the account service to build
 * account-level trade summaries that include the compliance state of each trade.
 */
@Entity
@Table(name = "TRADES")
public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(length = 100, name = "ID")
    @Id
    private String id;

    @Column(name = "ACCOUNTID")
    private Integer accountId;

    @Column(length = 50, name = "SECURITY")
    private String security;

    @Column(length = 4, name = "SIDE")
    private String side;

    @Column(length = 20, name = "STATE")
    private String state;

    @Column(length = 20, name = "COMPLIANCESTATUS")
    private String complianceStatus;

    @Column(name = "QUANTITY")
    private Integer quantity;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getAccountId() {
        return this.accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getSecurity() {
        return this.security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getSide() {
        return this.side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getComplianceStatus() {
        return this.complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
