// Local domain types for the API service layer.
// Kept self-contained (mirrors web-front-end/angular/main/app/model/*) so this
// piece does not depend on any other migration piece's folder.

export interface Account {
	id: number;
	displayName: string;
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

export interface Stock {
	ticker: string;
	companyName: string;
}
