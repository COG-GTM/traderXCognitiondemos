// Local data fetching for the Accounts piece.
// Follows the env.ts pattern: compute the account-service base URL from
// window.location.hostname. Account service lives on :18088.

import { Account, AccountUser } from './types';

const accountServiceUrl = `http://${window.location.hostname}:18088`;

const jsonHeaders = { 'Content-Type': 'application/json' };

export async function getAccounts(): Promise<Account[]> {
	const response = await fetch(`${accountServiceUrl}/account/`, {
		headers: jsonHeaders,
	});
	if (!response.ok) {
		throw new Error(`Failed to load accounts (${response.status})`);
	}
	return response.json();
}

export async function getAccountUsers(): Promise<AccountUser[]> {
	const response = await fetch(`${accountServiceUrl}/accountuser/`, {
		headers: jsonHeaders,
	});
	if (!response.ok) {
		throw new Error(`Failed to load account users (${response.status})`);
	}
	return response.json();
}

export async function addAccount(account: Partial<Account>): Promise<Partial<Account>> {
	const response = await fetch(`${accountServiceUrl}/account/`, {
		method: 'POST',
		headers: jsonHeaders,
		body: JSON.stringify(account),
	});
	if (!response.ok) {
		throw new Error(`Failed to save account (${response.status})`);
	}
	return response.json();
}
