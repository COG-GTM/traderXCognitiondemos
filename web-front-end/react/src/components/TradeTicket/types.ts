// Local, self-contained types for the Trade Ticket piece.
// Mirrors web-front-end/angular/main/app/model/{trade,symbol}.model.ts

export type Side = 'Buy' | 'Sell';

export interface Stock {
	ticker: string;
	companyName: string;
}

export interface TradeTicket {
	side: Side;
	quantity: number;
	security: string;
	accountId: number;
}
