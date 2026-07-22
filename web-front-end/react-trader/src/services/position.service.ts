import { environment } from '../environments/environment';
import type { Position, Trade } from '../models/trade.model';

const tradesUrl = `${environment.positionsUrl}/trades/`;
const positionsUrl = `${environment.positionsUrl}/positions/`;

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export function getTrades(accountId: number): Promise<Trade[]> {
  return fetchJson<Trade[]>(`${tradesUrl}${accountId}`);
}

export function getPositions(accountId: number): Promise<Position[]> {
  return fetchJson<Position[]>(`${positionsUrl}${accountId}`);
}
