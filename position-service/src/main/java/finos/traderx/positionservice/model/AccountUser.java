package finos.traderx.positionservice.model;

import java.io.Serializable;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Read-only view of the account-user mapping owned by account-service.
 */
@Entity
@Immutable
@IdClass(AccountUserID.class)
@Table(name = "ACCOUNTUSERS")
public class AccountUser implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ACCOUNTID")
	private Integer accountId;

	@Id
	@Column(length = 15, name = "USERNAME")
	private String username;

	public Integer getAccountId() {
		return this.accountId;
	}

	public String getUsername() {
		return this.username;
	}
}
