import { environment } from '../config/environment';
import { Account, AccountUser } from '../models';
import { getJson, postJson } from './http';

const baseUrl = environment.accountUrl;

export const accountService = {
  getAccounts(): Promise<Account[]> {
    return getJson<Account[]>(`${baseUrl}/account/`);
  },

  addAccount(account: Partial<Account>): Promise<Partial<Account>> {
    return postJson<Partial<Account>>(`${baseUrl}/account/`, account);
  },

  addAccountUser(accountUser: AccountUser): Promise<AccountUser> {
    return postJson<AccountUser>(`${baseUrl}/accountuser/`, accountUser);
  },

  getAccountUsers(): Promise<AccountUser[]> {
    return getJson<AccountUser[]>(`${baseUrl}/accountuser/`);
  },
};
