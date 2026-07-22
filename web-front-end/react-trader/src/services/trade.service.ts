import { environment } from '../environments/environment';
import type { Stock } from '../models/symbol.model';
import type { TradeTicket } from '../models/trade.model';

const stocksUrl = `${environment.refrenceDataUrl}/stocks`;
const createTicketUrl = `${environment.tradesUrl}`;

async function fetchJson<T>(url: string, init?: RequestInit, retries = 0): Promise<T> {
    try {
        const response = await fetch(url, init);
        if (!response.ok) {
            throw new Error(`Request to ${url} failed with status ${response.status}`);
        }
        return (await response.json()) as T;
    } catch (error) {
        if (retries > 0) {
            return fetchJson<T>(url, init, retries - 1);
        }
        console.error(error);
        throw error;
    }
}

export function getStocks(): Promise<Stock[]> {
    return fetchJson<Stock[]>(stocksUrl, undefined, 2);
}

export function createTicket(ticket: TradeTicket): Promise<unknown> {
    return fetchJson<unknown>(createTicketUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(ticket)
    });
}
