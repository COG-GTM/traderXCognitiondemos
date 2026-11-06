import { appendRow, toPositionRow, toTradeRow, TRUSTED_PUBLISHER } from './feedMessages';

const trade = {
	from: TRUSTED_PUBLISHER,
	type: 'Trade',
	topic: '/accounts/22214/trades',
	date: 1,
	payload: { id: 'T-1', accountId: 22214, security: 'IBM', side: 'Buy', state: 'Settled', quantity: 100, updated: 1700000000000, created: 1700000000000 },
};
const position = {
	from: TRUSTED_PUBLISHER,
	type: 'Position',
	topic: '/accounts/22214/positions',
	date: 1,
	payload: { accountId: 22214, security: 'IBM', quantity: 100, updated: 1700000000000 },
};

test('accepts trade and position updates from trade-processor for the selected account', () => {
	expect(toTradeRow(trade, 22214)).toEqual(trade.payload);
	expect(toTradeRow(trade, '22214')).toEqual(trade.payload);
	expect(toPositionRow(position, 22214)).toEqual(position.payload);
});

test('drops messages that were not published by trade-processor', () => {
	for (const from of ['System', 'trade-service', 'anonymous', 'aBcSocketId123', undefined]) {
		expect(toTradeRow({ ...trade, from }, 22214)).toBeNull();
		expect(toPositionRow({ ...position, from }, 22214)).toBeNull();
	}
});

test('drops System join/leave messages on the account topic', () => {
	const join = { from: 'System', type: 'message', topic: '/accounts/22214/positions', payload: { message: 'Subscribed to /accounts/22214/positions' } };
	expect(toPositionRow(join, 22214)).toBeNull();
});

test('drops messages for another topic, type or account', () => {
	expect(toTradeRow({ ...trade, topic: '/accounts/52355/trades' }, 22214)).toBeNull();
	expect(toTradeRow({ ...trade, type: 'Position' }, 22214)).toBeNull();
	expect(toTradeRow(position, 22214)).toBeNull();
	expect(toPositionRow(trade, 22214)).toBeNull();
	expect(toPositionRow({ ...position, payload: { ...position.payload, accountId: 52355 } }, 22214)).toBeNull();
});

test('drops malformed payloads', () => {
	const bad = [
		null, 'x', [], { ...trade, payload: null }, { ...trade, payload: [] },
		{ ...trade, payload: { ...trade.payload, security: '' } },
		{ ...trade, payload: { ...trade.payload, security: { $gt: '' } } },
		{ ...trade, payload: { ...trade.payload, security: 'X'.repeat(1000) } },
		{ ...trade, payload: { ...trade.payload, quantity: '100' } },
		{ ...trade, payload: { ...trade.payload, quantity: -100 } },
		{ ...trade, payload: { ...trade.payload, quantity: 1.5 } },
		{ ...trade, payload: { ...trade.payload, side: 'Short' } },
		{ ...trade, payload: { ...trade.payload, state: 'Pending' } },
		{ ...trade, payload: { ...trade.payload, updated: 'not a date' } },
	];
	bad.forEach((msg) => expect(toTradeRow(msg, 22214)).toBeNull());
	expect(toPositionRow({ ...position, payload: { ...position.payload, quantity: Infinity } }, 22214)).toBeNull();
});

test('copies only known fields into the grid row', () => {
	const row = toPositionRow({ ...position, payload: { ...position.payload, extra: '<img src=x onerror=alert(1)>' } }, 22214);
	expect(row).toEqual(position.payload);
	expect(row).not.toHaveProperty('extra');
});

test('appendRow keeps at most max rows', () => {
	expect(appendRow(undefined, 1)).toEqual([1]);
	expect(appendRow([1, 2, 3], 4, 3)).toEqual([2, 3, 4]);
	let rows: number[] = [];
	for (let i = 0; i < 10000; i++) rows = appendRow(rows, i);
	expect(rows.length).toBe(5000);
	expect(rows[4999]).toBe(9999);
});
