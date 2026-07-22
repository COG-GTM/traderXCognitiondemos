// Ported from web-front-end/angular/main/app/service/account.service.ts
// Account service (:18088).
import { ServiceUrls, httpJson } from './config';
import { Account, AccountUser } from './types';

const baseUrl = (): string => ServiceUrls.accountService;

export async function getAccounts(): Promise<Account[]> {
	return httpJson<Account[]>(`${baseUrl()}/account/`);
}

export async function addAccount(account: Partial<Account>): Promise<Partial<Account>> {
	return httpJson<Partial<Account>>(`${baseUrl()}/account/`, {
		method: 'POST',
		body: JSON.stringify(account),
	});
}

export async function addAccountUser(accountUser: AccountUser): Promise<AccountUser> {
	return httpJson<AccountUser>(`${baseUrl()}/accountuser/`, {
		method: 'POST',
		body: JSON.stringify(accountUser),
	});
}

export async function getAccountUsers(): Promise<AccountUser[]> {
	return httpJson<AccountUser[]>(`${baseUrl()}/accountuser/`);
}
