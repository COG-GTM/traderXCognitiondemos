import { fromMinorUnits, toMinorUnits } from './money';
import { CurrencySummary, ReportPosition } from './types';

/**
 * Per-currency summaries. Rules (CONTRACT_BRIEF.md, "Aggregation"):
 *  - never sum unlike currencies, never convert;
 *  - an unknown market value is excluded from the total and marks the summary `incomplete`.
 * Output is sorted by currency code for a deterministic render.
 */
export const summarizeByCurrency = (positions: ReadonlyArray<ReportPosition>): CurrencySummary[] => {
	const buckets = new Map<string, { count: number; total: bigint; incomplete: boolean }>();
	for (const p of positions) {
		const bucket = buckets.get(p.currency) ?? { count: 0, total: BigInt(0), incomplete: false };
		bucket.count += 1;
		if (p.marketValue === null) {
			bucket.incomplete = true;
		} else {
			if (p.marketValue.currency !== p.currency) {
				throw new Error(`Position ${p.security}: market value currency ${p.marketValue.currency} differs from ${p.currency}`);
			}
			bucket.total += toMinorUnits(p.marketValue.amount, p.currency);
		}
		buckets.set(p.currency, bucket);
	}
	return Array.from(buckets.entries())
		.sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
		.map(([currency, b]) => ({
			currency,
			count: b.count,
			totalMarketValue: fromMinorUnits(b.total, currency),
			incomplete: b.incomplete,
		}));
};

/** Deterministic row order for the report: security ascending (same order as the approved contract). */
export const sortPositions = (positions: ReadonlyArray<ReportPosition>): ReportPosition[] =>
	[...positions].sort((a, b) => (a.security < b.security ? -1 : a.security > b.security ? 1 : 0));
