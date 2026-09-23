import React from 'react';
import { render, screen, within, fireEvent, waitFor } from '@testing-library/react';
import { PortfolioReport, UNKNOWN_LABEL, INCOMPLETE_LABEL } from './PortfolioReport';
import { loadFixture, ReportExpectations } from './fixtures';
import { PositionData } from '../Datatable/types';
import { formatAmount } from './money';

/**
 * Consumer integration test: renders the Portfolio report against a MOCKED positions service and
 * checks it against the approved fixture expectations. The migrated consumer must keep this green.
 */
const expectations = loadFixture<ReportExpectations>('report-expectations.json');
const v1Rows = loadFixture<PositionData[]>('v1-positions-77007.json');

const mockFetch = (handler: (url: string) => unknown) => {
	global.fetch = jest.fn(async (input: RequestInfo | URL) => {
		const url = String(input);
		const body = handler(url);
		if (body === undefined) {
			return { ok: false, status: 404, json: async () => ({}) } as Response;
		}
		return { ok: true, status: 200, json: async () => body } as Response;
	}) as unknown as typeof fetch;
	return global.fetch as jest.Mock;
};

const legacyOnly = (url: string) => (url.endsWith(`/positions/${expectations.accountId}`) ? v1Rows : undefined);

const expectReportMatchesFixture = async () => {
	await waitFor(() =>
		expect(screen.getByTestId('position-count')).toHaveTextContent(String(expectations.rows.length))
	);
	const table = screen.getByTestId('positions-table');
	const renderedSecurities = within(table)
		.getAllByRole('row')
		.slice(1)
		.map((r) => within(r).getAllByRole('cell')[0].textContent);
	expect(renderedSecurities).toEqual(expectations.rows.map((r) => r.security));

	for (const row of expectations.rows) {
		const tr = screen.getByTestId(`position-${row.security}`);
		const cells = within(tr).getAllByRole('cell').map((c) => c.textContent);
		expect(cells[1]).toBe(String(row.quantity));
		expect(cells[2]).toBe(row.currency);
		expect(cells[3]).toBe(row.marketValue === null ? UNKNOWN_LABEL : formatAmount(row.marketValue));
		expect(cells[4]).toBe(row.asOf);
	}

	for (const s of expectations.currencySummaries) {
		const tr = screen.getByTestId(`summary-${s.currency}`);
		const cells = within(tr).getAllByRole('cell').map((c) => c.textContent);
		expect(cells).toEqual([s.currency, String(s.count), formatAmount(s.totalMarketValue), s.incomplete ? INCOMPLETE_LABEL : 'complete']);
	}
};

describe('Portfolio report (consumer integration)', () => {
	afterEach(() => jest.restoreAllMocks());

	test('shows the Synthetic demonstration label', () => {
		mockFetch(legacyOnly);
		render(<PortfolioReport />);
		expect(screen.getByTestId('synthetic-label')).toHaveTextContent(expectations.label);
	});

	test('current report matches the approved fixture expectations', async () => {
		mockFetch(legacyOnly);
		render(<PortfolioReport />);
		await expectReportMatchesFixture();
		expect(screen.getByTestId('report-source')).toHaveTextContent('current adapter');
	});

	test('legacy v1 comparison toggle renders the same approved expectations', async () => {
		const fetchMock = mockFetch(legacyOnly);
		render(<PortfolioReport />);
		fireEvent.click(screen.getByLabelText('Legacy v1 (comparison)'));
		await expectReportMatchesFixture();
		expect(screen.getByTestId('report-source')).toHaveTextContent('legacy v1 comparison');
		expect(fetchMock.mock.calls.some(([u]) => String(u).endsWith(`/positions/${expectations.accountId}`))).toBe(true);
	});

	test('unknown market values stay unknown and their currency totals are labeled incomplete', async () => {
		mockFetch(legacyOnly);
		render(<PortfolioReport />);
		await expectReportMatchesFixture();
		for (const sec of expectations.unknownMarketValueSecurities) {
			expect(screen.getByTestId(`position-${sec}`)).toHaveTextContent(UNKNOWN_LABEL);
		}
		const incompleteCurrencies = expectations.currencySummaries.filter((s) => s.incomplete).map((s) => s.currency);
		expect(incompleteCurrencies.length).toBeGreaterThan(0);
		for (const ccy of incompleteCurrencies) {
			expect(screen.getByTestId(`summary-${ccy}`)).toHaveTextContent(INCOMPLETE_LABEL);
		}
	});
});
