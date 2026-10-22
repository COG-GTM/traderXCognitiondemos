package finos.traderx.accountservice.dto;

/**
 * Client payload for creating or renaming an account. It deliberately has no id:
 * the id is generated on create and taken from the path on update.
 */
public class AccountRequest {

	public static final int MAX_DISPLAY_NAME_LENGTH = 50;

	private String displayName;

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
