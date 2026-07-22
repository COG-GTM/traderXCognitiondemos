import { environment } from '../environments/environment';
import type { Stock } from '../models/symbol.model';
import type { TradeTicket } from '../models/trade.model';
import { request } from './http';

const stocksUrl = `${environment.refrenceDataUrl}/stocks`;
const createTicketUrl = environment.tradesUrl;

export function getStocks(): Promise<Stock[]> {
  return request<Stock[]>(stocksUrl, { retries: 2 });
}

export function createTicket(ticket: TradeTicket): Promise<unknown> {
  return request<unknown>(createTicketUrl, { method: 'POST', body: ticket });
}

export const symbolService = { getStocks, createTicket };
export type SymbolService = typeof symbolService;
