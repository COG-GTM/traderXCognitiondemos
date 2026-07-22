// Ported from web-front-end/angular/main/app/service/position.service.ts
// Position service (:18090).
import { ServiceUrls, httpJson } from './config';
import { Trade, Position } from './types';

const baseUrl = (): string => ServiceUrls.positionService;

export async function getTrades(accountId: number): Promise<Trade[]> {
	return httpJson<Trade[]>(`${baseUrl()}/trades/${accountId}`);
}

export async function getPositions(accountId: number): Promise<Position[]> {
	return httpJson<Position[]>(`${baseUrl()}/positions/${accountId}`);
}
