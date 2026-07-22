// Local types for the Trade Blotter piece. Defined locally on purpose so this
// piece stays self-contained (see INTEGRATION NOTE in README.md). Mirrors
// web-front-end/angular/main/app/model/trade.model.ts.

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

// Shape of a message coming off the trade feed socket.
export interface FeedMessage<T = unknown> {
	from: string;
	topic: string;
	payload: T;
}
