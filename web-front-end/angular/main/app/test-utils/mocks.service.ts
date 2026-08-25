import { NEVER, Observable, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Account } from '../model/account.model';
import { AccountUser, User } from '../model/user.model';
import { createAccount, createUser, createStock, createTrade, createPosition } from './utils';
import { Stock } from '../model/symbol.model';
import { Trade, Position, TradeTicket } from '../model/trade.model';

export const accounts: Account[] = Array.from({ length: 5 }, () => createAccount());
export const stocks: Stock[] = Array.from({ length: 5 }, () => createStock());
export const trades: Trade[] = Array.from({ length: 2 }, () => createTrade());
export const positions: Position[] = Array.from({ length: 2 }, () => createPosition());
const users: User[] = Array.from({ length: 5 }, () => createUser());
const accountUsers: AccountUser[] = accounts.map((ac, index) => {
  const idx = Math.floor(Math.random() * (accounts.length - index));
  return { accountId: ac.id, username: users[idx].fullName };
});

export class MockAccountService {
  addAccountUser(accountUser: AccountUser) {
    return of<AccountUser>(accountUser);
  }

  addAccount(account: Partial<Account>) {
    return of<Account>({ displayName: account.displayName || '', id: 1 });
  }

  getAccounts() {
    return of<Account[]>(accounts);
  }

  getAccountUsers(): Observable<AccountUser[]> {
    return of<AccountUser[]>(accountUsers);
  }
}

export class MockUserService {
  getUsers(searchText: string): Observable<User[]> {
    const src = [{ fullName: 'Jhon mac' }, { fullName: 'Tom san' }, { fullName: 'Merry san' }] as User[];
    return of<User[]>(src.filter((u) => u.fullName.indexOf(searchText) !== -1));
  }
}

export class MockTradeService {

  getTrades(account_id: number): Observable<Trade[]> {
    return of(trades);
  }

  getPositions(account_id: number): Observable<Position[]> {
    return of(positions);
  }
}

export class MockSymbolService {

  getStocks() {
    return of(stocks);
  }

  createTicket(ticket: TradeTicket) {
    console.log('dummy create ticket called');
    return of({});
  }

}

/**
 * In-memory stand-in for the socket.io backed TradeFeedService.
 * Records the live subscriptions so tests can emit payloads on a topic and
 * assert that a subscription was torn down.
 */
export class MockTradeFeedService {
  subscriptions = new Map<string, Function[]>();
  unSubscribedTopics: string[] = [];

  subscribe(topic: string, callback: Function) {
    const callbacks = this.subscriptions.get(topic) ?? [];
    callbacks.push(callback);
    this.subscriptions.set(topic, callbacks);
    return () => this.unSubscribe(topic, callback);
  }

  unSubscribe(topic?: string, callback?: Function) {
    if (!topic) {
      return;
    }
    this.unSubscribedTopics.push(topic);
    const callbacks = (this.subscriptions.get(topic) ?? []).filter((cb) => cb !== callback);
    if (callbacks.length) {
      this.subscriptions.set(topic, callbacks);
    } else {
      this.subscriptions.delete(topic);
    }
  }

  /** Push a payload to every live subscriber of a topic, like the server would. */
  emit(topic: string, payload: any) {
    (this.subscriptions.get(topic) ?? []).forEach((cb) => cb(payload));
  }

  isSubscribed(topic: string) {
    return (this.subscriptions.get(topic) ?? []).length > 0;
  }
}

/** PositionService stand-in whose calls always fail, for HTTP error path tests. */
export class MockFailingPositionService {
  error = new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' });

  getTrades(_accountId: number): Observable<Trade[]> {
    return throwError(() => this.error);
  }

  getPositions(_accountId: number): Observable<Position[]> {
    return throwError(() => this.error);
  }
}

/** PositionService stand-in whose calls never answer, for request timeout tests. */
export class MockHangingPositionService {
  getTrades(_accountId: number): Observable<Trade[]> {
    return NEVER;
  }

  getPositions(_accountId: number): Observable<Position[]> {
    return NEVER;
  }
}
