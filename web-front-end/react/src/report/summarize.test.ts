import { loadFixture, ReportExpectations, V2Page } from './fixtures';
import { summarizeByCurrency, sortPositions } from './summarize';
import { formatAmount, fromMinorUnits, sumAmounts, toMinorUnits } from './money';
import { ReportPosition } from './types';

/**
 * Approved fixture expectations expressed against the report model. These are contract-level:
 * a consumer fed from the v2 pages (all cursors walked) must produce exactly these rows and summaries.
 */
const expectations = loadFixture<ReportExpectations>('report-expectations.json');
// TECHFEST_SCENARIO=seeded-defect swaps in the presenter-only defective page set (expected to FAIL).
const v2Pages =
	process.env.TECHFEST_SCENARIO === 'seeded-defect'
		? loadFixture<{ pages: V2Page[] }>('v2-pages-77007-seeded-defect.json').pages
		: loadFixture<V2Page[]>('v2-pages-77007.json');

const fromV2Pages = (pages: V2Page[]): ReportPosition[] =>
	pages.flatMap((p) =>
		p.response.items.map((i) => ({
			accountId: i.accountId,
			security: i.security,
			quantity: i.quantity,
			asOf: i.asOf,
			currency: i.currency,
			marketValue: i.marketValue,
		}))
	);

describe('approved fixture expectations', () => {
	test('v2 fixture has the required page shape (>=3 pages, unknown only after page 1, terminal null cursor)', () => {
		expect(v2Pages.length).toBe(expectations.expectedPageCount);
		expect(v2Pages.length).toBeGreaterThanOrEqual(3);
		expect(v2Pages[0].response.items.every((i) => i.marketValue !== null)).toBe(true);
		expect(v2Pages.slice(1).some((p) => p.response.items.some((i) => i.marketValue === null))).toBe(true);
		expect(v2Pages[v2Pages.length - 1].response.nextCursor).toBeNull();
		v2Pages.slice(0, -1).forEach((p) => expect(typeof p.response.nextCursor).toBe('string'));
	});

	test('walking every v2 page yields each expected position exactly once, in stable order', () => {
		const positions = sortPositions(fromV2Pages(v2Pages));
		expect(positions.map((p) => p.security)).toEqual(expectations.rows.map((r) => r.security));
		expect(new Set(positions.map((p) => p.security)).size).toBe(expectations.rows.length);
		positions.forEach((p, i) => {
			const r = expectations.rows[i];
			expect(p.quantity).toBe(r.quantity);
			expect(p.asOf).toBe(r.asOf);
			expect(p.currency).toBe(r.currency);
			expect(p.marketValue === null ? null : p.marketValue.amount).toBe(r.marketValue);
		});
	});

	test('per-currency summaries match the approved totals and incomplete flags', () => {
		expect(summarizeByCurrency(fromV2Pages(v2Pages))).toEqual(expectations.currencySummaries);
	});

	test('dropping a later page changes the summaries (guards against silent page loss)', () => {
		const truncated = summarizeByCurrency(fromV2Pages(v2Pages.slice(0, 1)));
		expect(truncated).not.toEqual(expectations.currencySummaries);
	});
});

describe('decimal-safe money', () => {
	test('minor-unit round trips keep exact decimals', () => {
		expect(fromMinorUnits(toMinorUnits('1234567.89', 'EUR'), 'EUR')).toBe('1234567.89');
		expect(fromMinorUnits(toMinorUnits('0.10', 'USD') + toMinorUnits('0.20', 'USD'), 'USD')).toBe('0.30');
		expect(fromMinorUnits(toMinorUnits('8460000', 'JPY'), 'JPY')).toBe('8460000');
		expect(fromMinorUnits(toMinorUnits('-49500.00', 'USD'), 'USD')).toBe('-49500.00');
	});

	test('rejects mixed-currency sums and over-precise amounts', () => {
		expect(() => sumAmounts([{ amount: '1.00', currency: 'EUR' }], 'USD')).toThrow(/Refusing to sum/);
		expect(() => toMinorUnits('1.5', 'JPY')).toThrow(/more than 0 decimals/);
	});

	test('formats with separators without float round-trip', () => {
		expect(formatAmount('35860000')).toBe('35,860,000');
		expect(formatAmount('-49500.00')).toBe('-49,500.00');
		expect(formatAmount('567180.00')).toBe('567,180.00');
	});
});
