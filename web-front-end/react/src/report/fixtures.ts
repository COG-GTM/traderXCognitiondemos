import fs from 'fs';
import path from 'path';

/** Approved scenario fixtures shared with position-service (single source of truth). Test-only helper. */
export const FIXTURE_DIR = path.resolve(__dirname, '../../../../position-service/techfest-migration/fixtures');

export const loadFixture = <T,>(name: string): T =>
	JSON.parse(fs.readFileSync(path.join(FIXTURE_DIR, name), 'utf8')) as T;

export interface ExpectedRow {
	security: string;
	quantity: number;
	asOf: string;
	currency: string;
	marketValue: string | null;
}

export interface ExpectedSummary {
	currency: string;
	count: number;
	totalMarketValue: string;
	incomplete: boolean;
}

export interface ReportExpectations {
	label: string;
	accountId: number;
	pageSize: number;
	expectedPageCount: number;
	unknownMarketValueSecurities: string[];
	rows: ExpectedRow[];
	currencySummaries: ExpectedSummary[];
}

export interface V2Page {
	request: { accountId: number; limit: number; cursor: string | null };
	response: V2PageResponse;
}

export interface V2PageResponse {
	items: Array<{
		accountId: number;
		security: string;
		quantity: number;
		asOf: string;
		currency: string;
		marketValue: { amount: string; currency: string } | null;
	}>;
	nextCursor: string | null;
}
