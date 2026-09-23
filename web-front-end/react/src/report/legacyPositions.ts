import { useEffect, useState } from 'react';
import { Environment } from '../env';
import { PositionData } from '../Datatable/types';
import { fromLegacyPosition } from './legacyAdapter';
import { ReportPosition } from './types';

/**
 * COMPARISON BASELINE ONLY. Reads the deprecated v1 endpoint so the Portfolio report can show the
 * legacy report next to the current one. This is the one place v1 is allowed to remain after the
 * migration (CONTRACT_BRIEF.md, "Compatibility"). Do not use it on the primary execution path.
 */
export const fetchLegacyPositions = async (accountId: number): Promise<ReportPosition[]> => {
	const response = await fetch(`${Environment.position_service_url}/positions/${accountId}`);
	if (!response.ok) {
		throw new Error(`Legacy positions request failed: ${response.status}`);
	}
	const rows: PositionData[] = await response.json();
	return rows.map(fromLegacyPosition);
};

export const useLegacyPositions = (accountId: number, enabled: boolean) => {
	const [positions, setPositions] = useState<ReportPosition[]>([]);
	const [error, setError] = useState<string | null>(null);
	useEffect(() => {
		if (!enabled || !accountId) {
			return;
		}
		let cancelled = false;
		fetchLegacyPositions(accountId)
			.then((p) => !cancelled && (setPositions(p), setError(null)))
			.catch((e: Error) => !cancelled && setError(e.message));
		return () => {
			cancelled = true;
		};
	}, [accountId, enabled]);
	return { positions, error };
};
