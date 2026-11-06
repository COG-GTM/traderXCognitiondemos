import { PositionData, TradeData, TradeSide } from './types';

// The trade-feed stamps `from` with the authenticated service identity; only trade-processor publishes account updates.
export const TRUSTED_PUBLISHER = 'trade-processor';
export const MAX_ROWS = 5000;

const TRADE_SIDES: TradeSide[] = ['Buy', 'Sell'];
const TRADE_STATES = ['New', 'Processing', 'Settled', 'Cancelled'];
const MAX_SECURITY_LENGTH = 32;

type FeedRecord = { [key: string]: unknown };

const isRecord = (value: unknown): value is FeedRecord =>
	typeof value === 'object' && value !== null && !Array.isArray(value);

const isQuantity = (value: unknown): value is number =>
	typeof value === 'number' && Number.isSafeInteger(value);

const isSecurity = (value: unknown): value is string =>
	typeof value === 'string' && value.length > 0 && value.length <= MAX_SECURITY_LENGTH;

const isTimestamp = (value: unknown): boolean =>
	value === undefined || value === null || (typeof value === 'number' && Number.isFinite(value)) || (typeof value === 'string' && !Number.isNaN(Date.parse(value)));

function trustedPayload(message: unknown, accountId: number | string, kind: 'trades' | 'positions', type: string): FeedRecord | null {
	if (!isRecord(message)) return null;
	if (message.from !== TRUSTED_PUBLISHER || message.type !== type) return null;
	if (message.topic !== `/accounts/${accountId}/${kind}`) return null;
	const payload = message.payload;
	if (!isRecord(payload) || payload.accountId !== Number(accountId)) return null;
	return payload;
}

export function toTradeRow(message: unknown, accountId: number | string): TradeData | null {
	const p = trustedPayload(message, accountId, 'trades', 'Trade');
	if (!p) return null;
	if (!isSecurity(p.security) || !isQuantity(p.quantity) || p.quantity <= 0) return null;
	if (!TRADE_SIDES.includes(p.side as TradeSide) || !TRADE_STATES.includes(p.state as string)) return null;
	if (p.id !== undefined && typeof p.id !== 'string') return null;
	if (!isTimestamp(p.updated) || !isTimestamp(p.created)) return null;
	return {
		id: p.id as string | undefined,
		accountId: p.accountId as number,
		security: p.security,
		side: p.side as TradeSide,
		state: p.state as string,
		quantity: p.quantity,
		updated: p.updated as Date | undefined,
		created: p.created as Date | undefined,
	};
}

export function toPositionRow(message: unknown, accountId: number | string): PositionData | null {
	const p = trustedPayload(message, accountId, 'positions', 'Position');
	if (!p) return null;
	if (!isSecurity(p.security) || !isQuantity(p.quantity) || !isTimestamp(p.updated)) return null;
	return {
		accountId: p.accountId as number,
		security: p.security,
		quantity: p.quantity,
		updated: p.updated as Date,
	};
}

export function appendRow<T>(rows: T[] | undefined, row: T, max: number = MAX_ROWS): T[] {
	const next = [...(rows || []), row];
	return next.length > max ? next.slice(next.length - max) : next;
}
