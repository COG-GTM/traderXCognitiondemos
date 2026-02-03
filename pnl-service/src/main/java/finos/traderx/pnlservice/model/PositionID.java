package finos.traderx.pnlservice.model;

import java.io.Serializable;

public class PositionID implements Serializable {
	private Integer accountId;
	private String security;

	public PositionID() {
	}

	public PositionID(Integer accountId, String security) {
			this.accountId = accountId;
			this.security = security;
	}
}
