import type { Account } from '../models/account.model';
import type { Stock } from '../models/symbol.model';
import type { Position, Trade, TradeTicket } from '../models/trade.model';
import type { AccountUser, User } from '../models/user.model';
import { createAccount, createPosition, createStock, createTrade, createUser } from './utils';

export const accounts: Account[] = Array.from({ length: 5 }, () => createAccount());
export const stocks: Stock[] = Array.from({ length: 5 }, () => createStock());
export const trades: Trade[] = Array.from({ length: 2 }, () => createTrade());
export const positions: Position[] = Array.from({ length: 2 }, () => createPosition());
const users: User[] = Array.from({ length: 5 }, () => createUser());
const accountUsers: AccountUser[] = accounts.map((ac, index) => {
  const idx = Math.floor(Math.random() * (accounts.length - index));
  return { accountId: ac.id, username: users[idx].fullName };
});

export const mockAccountService = {
  addAccountUser(accountUser: AccountUser): Promise<AccountUser> {
    return Promise.resolve(accountUser);
  },

  addAccount(account: Partial<Account>): Promise<Account> {
    return Promise.resolve({ displayName: account.displayName || '', id: 1 });
  },

  getAccounts(): Promise<Account[]> {
    return Promise.resolve(accounts);
  },

  getAccountUsers(): Promise<AccountUser[]> {
    return Promise.resolve(accountUsers);
  }
};

export const mockUserService = {
  getUsers(searchText: string): Promise<User[]> {
    const src = [{ fullName: 'Jhon mac' }, { fullName: 'Tom san' }, { fullName: 'Merry san' }] as User[];
    return Promise.resolve(src.filter((u) => u.fullName.indexOf(searchText) !== -1));
  }
};

export const mockPositionService = {
  getTrades(_accountId: number): Promise<Trade[]> {
    return Promise.resolve(trades);
  },

  getPositions(_accountId: number): Promise<Position[]> {
    return Promise.resolve(positions);
  }
};

export const mockSymbolService = {
  getStocks(): Promise<Stock[]> {
    return Promise.resolve(stocks);
  },

  createTicket(_ticket: TradeTicket): Promise<unknown> {
    return Promise.resolve({});
  }
};

export const mockTradeFeedService = {
  subscribe(_topic: string, _callback: (...args: any[]) => void): () => void {
    return () => {};
  },

  unSubscribe(_topic: string, _callback: (...args: any[]) => void): void {}
};
