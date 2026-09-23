import { PositionData } from '../Datatable/types';
import { scaleFor } from './money';
import { ReportPosition } from './types';

/**
 * Maps one LEGACY (v1) position row onto the report model.
 * v1 carries `updated` (legacy timestamp string) and `marketValue` as a JSON number or null.
 * The number is rendered to the currency scale immediately so no float leaves this function.
 */
export const fromLegacyPosition = (row: PositionData): ReportPosition => {
	const currency = row.currency ?? 'USD';
	const marketValue =
		row.marketValue === null || row.marketValue === undefined
			? null
			: { amount: row.marketValue.toFixed(scaleFor(currency)), currency };
	return {
		accountId: row.accountId,
		security: row.security,
		quantity: row.quantity,
		asOf: toIsoUtc(row.updated),
		currency,
		marketValue,
	};
};

export const toIsoUtc = (value: Date | string | number | undefined): string => {
	if (value === undefined || value === null) {
		return '';
	}
	const date = value instanceof Date ? value : new Date(value);
	return date.toISOString().replace(/\.\d{3}Z$/, 'Z');
};
