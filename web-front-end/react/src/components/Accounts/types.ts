// Local types for the Accounts piece (React migration 9/10).
// Kept self-contained per integration rules; small duplication with other
// pieces is expected and will be de-duped during integration.

export interface Account {
	id: number;
	displayName: string;
}

export interface AccountUser {
	username: string;
	accountId: number;
}

export interface AccountResponse {
	success: boolean;
	msg: string;
}
