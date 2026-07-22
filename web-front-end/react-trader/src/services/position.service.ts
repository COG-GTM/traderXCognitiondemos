import { environment } from '../environments/environment';
import type { Position, Trade } from '../models/trade.model';
import { request } from './http';

const tradesUrl = `${environment.positionsUrl}/trades/`;
const positionsUrl = `${environment.positionsUrl}/positions/`;

export function getTrades(accountId: number): Promise<Trade[]> {
  return request<Trade[]>(tradesUrl + accountId);
}

export function getPositions(accountId: number): Promise<Position[]> {
  return request<Position[]>(positionsUrl + accountId);
}

export const positionService = { getTrades, getPositions };
export type PositionService = typeof positionService;
