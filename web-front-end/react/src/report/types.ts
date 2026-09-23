/** Decimal amount as a STRING at the currency's minor-unit scale, plus an ISO-4217 code. Never a float. */
export interface MarketValue {
	amount: string;
	currency: string;
}

/** Internal report model. `marketValue` is null when unknown; it is never 0 and never omitted. */
export interface ReportPosition {
	accountId: number;
	security: string;
	quantity: number;
	asOf: string; // ISO-8601 UTC, e.g. 2026-09-22T16:00:00Z
	currency: string;
	marketValue: MarketValue | null;
}

export interface CurrencySummary {
	currency: string;
	count: number;
	totalMarketValue: string; // sum of KNOWN values only, at currency scale
	incomplete: boolean; // true when at least one position in this currency has an unknown market value
}

export type ReportSource = 'current' | 'legacy';
