import { describeReason, formatRejectionMessage, parseTradeRejection } from './tradeRejection';

const rejectionBody = {
	decision: 'REJECTED',
	reason: 'NOTIONAL_LIMIT_BREACH',
	limit: 1000000,
	attempted: 1250000
};

test('parses a structured 422 body only', () => {
	expect(parseTradeRejection(422, rejectionBody)).toEqual(rejectionBody);
	expect(parseTradeRejection(404, rejectionBody)).toBeUndefined();
	expect(parseTradeRejection(422, 'Trade not found')).toBeUndefined();
	expect(parseTradeRejection(422, { message: 'nope' })).toBeUndefined();
});

test('names the limit, the attempt and the excess as currency', () => {
	expect(formatRejectionMessage(rejectionBody)).toEqual(
		'Order rejected: notional limit breach. Account limit $1,000,000.00, this order $1,250,000.00 — '
		+ 'over by $250,000.00. Amend the order and resubmit.'
	);
});

test('omits the excess when the attempt does not exceed the limit', () => {
	expect(formatRejectionMessage({ ...rejectionBody, attempted: 1000000 })).toEqual(
		'Order rejected: notional limit breach. Account limit $1,000,000.00, this order $1,000,000.00. '
		+ 'Amend the order and resubmit.'
	);
});

test('parses a 422 whose body arrived as an unparsed JSON string', () => {
	expect(parseTradeRejection(422, JSON.stringify(rejectionBody))).toEqual(rejectionBody);
	expect(parseTradeRejection(422, 'Trade rejected')).toBeUndefined();
});

test('unwraps a body that carries the raw response under text', () => {
	expect(parseTradeRejection(422, { error: new SyntaxError('Unexpected token'), text: JSON.stringify(rejectionBody) }))
		.toEqual(rejectionBody);
});

test('recognises a known reason code whatever its case', () => {
	expect(describeReason('notional_limit_breach')).toEqual('notional limit breach');
	expect(describeReason('Restricted_Security')).toEqual('restricted security');
});

test('does not resolve an inherited object property to its function source', () => {
	expect(formatRejectionMessage({ decision: 'REJECTED', reason: 'constructor' })).toEqual(
		'Order rejected: constructor. Amend the order and resubmit.'
	);
});

test('does not echo a free-text reason back to the trader', () => {
	expect(formatRejectionMessage({ decision: 'REJECTED', reason: 'x'.repeat(500) })).toEqual(
		'Order rejected: a pre-trade risk check. Amend the order and resubmit.'
	);
});

test('humanises an unknown reason code and copes with missing amounts', () => {
	expect(formatRejectionMessage({ decision: 'REJECTED', reason: 'RESTRICTED_SECURITY' })).toEqual(
		'Order rejected: restricted security. Amend the order and resubmit.'
	);
});
