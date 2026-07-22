// Self-contained service base URLs + data fetching for this piece, following
// the pattern in `src/env.ts` (compute from window.location.hostname).
// NOTE: people service is :18089 per the Angular source of truth
// (environment.ts), NOT :18095 as the shared env.ts currently lists.

import { AccountUser, User } from './types';

const host = window.location.hostname;
const PEOPLE_SERVICE_URL = `http://${host}:18089`;
const ACCOUNT_SERVICE_URL = `http://${host}:18088`;

/** Query the people service typeahead. Returns [] on error. */
export async function getMatchingPeople(searchText: string): Promise<User[]> {
	try {
		const params = new URLSearchParams({ SearchText: searchText, Take: '10' });
		const response = await fetch(
			`${PEOPLE_SERVICE_URL}/People/GetMatchingPeople?${params.toString()}`,
			{ headers: { 'Content-Type': 'application/json' } }
		);
		if (!response.ok) {
			throw new Error(`People service responded ${response.status}`);
		}
		const data: { people?: User[] } = await response.json();
		return data.people || [];
	} catch (err) {
		console.log((err as Error)?.message || 'Something goes wrong');
		return [];
	}
}

/** Assign a user to an account via the account service. */
export async function addAccountUser(accountUser: AccountUser): Promise<void> {
	const response = await fetch(`${ACCOUNT_SERVICE_URL}/accountuser/`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(accountUser),
	});
	if (!response.ok) {
		throw new Error(`Account service responded ${response.status}`);
	}
}
