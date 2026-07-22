export interface Account {
  id: number;
  displayName: string;
}

export interface Symbol {
  name: string;
  sector: string;
  symbol: string;
}

export interface Stock {
  ticker: string;
  companyName: string;
}

export enum Side {
  Sell = 'Sell',
  Buy = 'Buy',
}

export enum State {
  New = 'New',
  Processing = 'Processing',
  Pending = 'Pending',
  Settled = 'Settled',
}

export interface Trade {
  accountid: number;
  created: Date;
  id: string;
  quantity: number;
  security: string;
  side: Side;
  state: State;
  updated: Date;
}

export interface Position {
  accountid: number;
  quantity: number;
  security: string;
  updated: Date;
}

export interface TradeTicket {
  side: 'Sell' | 'Buy';
  quantity: number;
  security: string;
  accountId: number;
}

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
