// Self-contained service base URLs + data fetching for the Trade Ticket piece.
// Follows the pattern in web-front-end/react/src/env.ts (compute from window.location.hostname).
// Kept local so this piece does not depend on other pieces; will be de-duped at integration.

import { Stock, TradeTicket } from './types';

const hostname =
	typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const ServiceUrls = {
	reference_data_url: `http://${hostname}:18085`,
	trade_service_url: `http://${hostname}:18092`,
};

export async function fetchStocks(): Promise<Stock[]> {
	const response = await fetch(`${ServiceUrls.reference_data_url}/stocks`);
	if (!response.ok) {
		throw new Error(`Failed to load stocks (${response.status})`);
	}
	return (await response.json()) as Stock[];
}

export async function createTrade(ticket: TradeTicket): Promise<void> {
	const response = await fetch(`${ServiceUrls.trade_service_url}/trade/`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(ticket),
	});
	if (!response.ok) {
		throw new Error(`Failed to create trade (${response.status})`);
	}
}
