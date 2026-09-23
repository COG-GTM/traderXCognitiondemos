import { useMemo } from 'react';
import { GetPositions } from '../hooks';
import { fromLegacyPosition } from './legacyAdapter';
import { ReportPosition } from './types';

/**
 * Positions for the CURRENT report, sourced through the shared positions adapter (hooks/GetPositions.ts).
 * The adapter returns the legacy row shape today, so rows are mapped through the legacy adapter here.
 */
export const useReportPositions = (accountId: number): ReportPosition[] => {
	const rows = GetPositions(accountId);
	return useMemo(() => rows.map(fromLegacyPosition), [rows]);
};
