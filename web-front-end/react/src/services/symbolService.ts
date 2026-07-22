import { environment } from '../config/environment';
import { Stock, TradeTicket } from '../models';
import { getJson, postJson } from './http';

const stocksUrl = `${environment.referenceDataUrl}/stocks`;
const createTicketUrl = environment.tradesUrl;

export const symbolService = {
  getStocks(): Promise<Stock[]> {
    return getJson<Stock[]>(stocksUrl);
  },

  createTicket(ticket: TradeTicket): Promise<unknown> {
    return postJson<unknown>(createTicketUrl, ticket);
  },
};
