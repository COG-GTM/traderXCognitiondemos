// Local, self-contained types for the AssignUser component.
// Mirrors the Angular models (user.model.ts / account.model.ts). Duplicated
// intentionally to keep this piece independent of the other migration pieces.

export interface User {
	logonId: string;
	fullName: string;
	email: string;
	employeeId: string;
	department: string;
	photoUrl: string;
}

export interface AccountUser {
	username: string;
	accountId: number;
}

export interface Account {
	id: number;
	displayName: string;
}

export interface AssignUserProps {
	/** Currently selected account (may be preset from the parent). */
	account?: Account;
	/** All accounts available for selection. */
	accounts: Account[];
	/** Emitted after a user is successfully added (Angular `update` output). */
	onUpdate?: (account: Account) => void;
}

export interface AddUserResponse {
	success?: boolean;
	error?: boolean;
	msg: string;
}
