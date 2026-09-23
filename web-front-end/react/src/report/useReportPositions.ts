import { GetPositions } from '../hooks';
import { ReportPosition } from './types';

/**
 * Positions for the CURRENT report, sourced through the shared positions adapter (hooks/GetPositions.ts).
 * The adapter walks every v2 page and returns items already in the report model shape
 * (`asOf` ISO-8601 UTC, `marketValue` as {amount: string, currency} or null).
 */
export const useReportPositions = (accountId: number): ReportPosition[] => GetPositions(accountId);
