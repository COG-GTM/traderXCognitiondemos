import { environment } from '../config/environment';
import { Position, Trade } from '../models';
import { getJson } from './http';

const tradesUrl = `${environment.positionsUrl}/trades/`;
const positionsUrl = `${environment.positionsUrl}/positions/`;

export const positionService = {
  getTrades(accountId: number): Promise<Trade[]> {
    return getJson<Trade[]>(tradesUrl + accountId);
  },

  getPositions(accountId: number): Promise<Position[]> {
    return getJson<Position[]>(positionsUrl + accountId);
  },
};
