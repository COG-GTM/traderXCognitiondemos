import React, { useState } from 'react';
import { formatAmount } from './money';
import { sortPositions, summarizeByCurrency } from './summarize';
import { useLegacyPositions } from './legacyPositions';
import { useReportPositions } from './useReportPositions';
import { ReportPosition, ReportSource } from './types';
import './PortfolioReport.css';

export const DEFAULT_REPORT_ACCOUNT = 77007;
export const UNKNOWN_LABEL = 'unknown';
export const INCOMPLETE_LABEL = 'incomplete';

interface Props {
	initialAccountId?: number;
}

export const PortfolioReport = ({ initialAccountId = DEFAULT_REPORT_ACCOUNT }: Props) => {
	const [accountId, setAccountId] = useState<number>(initialAccountId);
	const [source, setSource] = useState<ReportSource>('current');
	const current = useReportPositions(accountId);
	const legacy = useLegacyPositions(accountId, source === 'legacy');
	const positions = source === 'legacy' ? legacy.positions : current;

	return (
		<section className="portfolio-report" aria-labelledby="portfolio-report-title">
			<header className="portfolio-report__header">
				<div>
					<h2 id="portfolio-report-title">Portfolio report</h2>
					<span className="portfolio-report__badge" data-testid="synthetic-label">Synthetic demonstration</span>
				</div>
				<div className="portfolio-report__controls">
					<label>
						Account
						<input
							type="number"
							aria-label="Account ID"
							value={accountId}
							onChange={(e) => setAccountId(Number(e.target.value))}
						/>
					</label>
					<fieldset className="portfolio-report__toggle">
						<legend>Report source</legend>
						<label>
							<input type="radio" name="report-source" value="current" checked={source === 'current'} onChange={() => setSource('current')} />
							Current
						</label>
						<label>
							<input type="radio" name="report-source" value="legacy" checked={source === 'legacy'} onChange={() => setSource('legacy')} />
							Legacy v1 (comparison)
						</label>
					</fieldset>
				</div>
			</header>

			<p className="portfolio-report__meta" data-testid="report-meta">
				Source: <strong data-testid="report-source">{source === 'legacy' ? 'legacy v1 comparison' : 'current adapter'}</strong>
				{' · '}Positions: <strong data-testid="position-count">{positions.length}</strong>
				{legacy.error && source === 'legacy' ? <span role="alert"> · {legacy.error}</span> : null}
			</p>

			<h3>Per-currency summary</h3>
			<table className="portfolio-report__table" data-testid="summary-table">
				<thead>
					<tr><th>Currency</th><th>Count</th><th>Total market value</th><th>Status</th></tr>
				</thead>
				<tbody>
					{summarizeByCurrency(positions).map((s) => (
						<tr key={s.currency} data-testid={`summary-${s.currency}`}>
							<td>{s.currency}</td>
							<td>{s.count}</td>
							<td className="num">{formatAmount(s.totalMarketValue)}</td>
							<td>{s.incomplete ? <span className="portfolio-report__flag">{INCOMPLETE_LABEL}</span> : 'complete'}</td>
						</tr>
					))}
				</tbody>
			</table>

			<h3>Positions</h3>
			<div className="portfolio-report__scroll">
				<table className="portfolio-report__table" data-testid="positions-table">
					<thead>
						<tr><th>Security</th><th>Quantity</th><th>Currency</th><th>Market value</th><th>As of (UTC)</th></tr>
					</thead>
					<tbody>
						{sortPositions(positions).map((p: ReportPosition) => (
							<tr key={p.security} data-testid={`position-${p.security}`}>
								<td>{p.security}</td>
								<td className="num">{p.quantity}</td>
								<td>{p.currency}</td>
								<td className="num">
									{p.marketValue === null ? <span className="portfolio-report__flag">{UNKNOWN_LABEL}</span> : formatAmount(p.marketValue.amount)}
								</td>
								<td>{p.asOf}</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</section>
	);
};
