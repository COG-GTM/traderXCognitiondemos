import { environment } from '../environments/environment';
import type { Account } from '../models/account.model';
import type { AccountUser } from '../models/user.model';
import { request } from './http';

const baseUrl = environment.accountUrl;

export function getAccounts(): Promise<Account[]> {
  return request<Account[]>(`${baseUrl}/account/`, { retries: 2 });
}

export function addAccount(account: Partial<Account>): Promise<Partial<Account>> {
  return request<Partial<Account>>(`${baseUrl}/account/`, { method: 'POST', body: account });
}

export function addAccountUser(accountUser: AccountUser): Promise<AccountUser> {
  return request<AccountUser>(`${baseUrl}/accountuser/`, { method: 'POST', body: accountUser });
}

export function getAccountUsers(): Promise<AccountUser[]> {
  return request<AccountUser[]>(`${baseUrl}/accountuser/`);
}

export const accountService = { getAccounts, addAccount, addAccountUser, getAccountUsers };
export type AccountService = typeof accountService;
