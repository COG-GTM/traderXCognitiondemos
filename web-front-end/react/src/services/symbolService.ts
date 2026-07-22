// Ported from web-front-end/angular/main/app/service/symbols.service.ts
// Reference data (:18085) for stocks; trade service (:18092) for ticket creation.
import { ServiceUrls, httpJson } from './config';
import { Stock, TradeTicket } from './types';

export async function getStocks(): Promise<Stock[]> {
	return httpJson<Stock[]>(`${ServiceUrls.referenceData}/stocks`);
}

export async function createTicket(ticket: TradeTicket): Promise<unknown> {
	return httpJson<unknown>(`${ServiceUrls.tradeService}/trade/`, {
		method: 'POST',
		body: JSON.stringify(ticket),
	});
}
