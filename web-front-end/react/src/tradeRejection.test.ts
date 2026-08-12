import { formatRejectionMessage, parseTradeRejection } from './tradeRejection';

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

test('humanises an unknown reason code and copes with missing amounts', () => {
	expect(formatRejectionMessage({ decision: 'REJECTED', reason: 'RESTRICTED_SECURITY' })).toEqual(
		'Order rejected: restricted security. Amend the order and resubmit.'
	);
});
