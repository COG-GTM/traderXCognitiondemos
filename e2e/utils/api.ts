import { APIRequestContext, request } from '@playwright/test';

/**
 * REST helpers for API/DB cross-checks. These hit the directly-exposed service
 * ports (also reachable through the ingress) so a spec can prove a trade
 * actually persisted server-side, independent of the UI.
 */

const HOST = process.env.TRADERX_HOST ?? 'localhost';

export const PORTS = {
  ingress: 8080,
  referenceData: 18085,
  account: 18088,
  position: 18090, // serves /trades/<id> and /positions/<id>
  trade: 18092, // serves POST /trade/
} as const;

export type Side = 'Buy' | 'Sell';

export interface Trade {
  accountid: number;
  created: string;
  id: string;
  quantity: number;
  security: string;
  side: Side;
  state: string;
  updated: string;
}

export interface Position {
  accountid: number;
  quantity: number;
  security: string;
  updated: string;
}

export class TraderXApi {
  private constructor(private ctx: APIRequestContext) {}

  static async create(): Promise<TraderXApi> {
    const ctx = await request.newContext();
    return new TraderXApi(ctx);
  }

  async dispose(): Promise<void> {
    await this.ctx.dispose();
  }

  private url(port: number, path: string): string {
    return `http://${HOST}:${port}${path}`;
  }

  async getTrades(accountId: number): Promise<Trade[]> {
    const res = await this.ctx.get(this.url(PORTS.position, `/trades/${accountId}`));
    if (!res.ok()) throw new Error(`GET /trades/${accountId} -> ${res.status()}`);
    return (await res.json()) as Trade[];
  }

  async getPositions(accountId: number): Promise<Position[]> {
    const res = await this.ctx.get(this.url(PORTS.position, `/positions/${accountId}`));
    if (!res.ok()) throw new Error(`GET /positions/${accountId} -> ${res.status()}`);
    return (await res.json()) as Position[];
  }

  async getPositionQuantity(accountId: number, security: string): Promise<number> {
    const positions = await this.getPositions(accountId);
    const match = positions.find((p) => p.security === security);
    return match ? match.quantity : 0;
  }

  async getAccounts(): Promise<Array<{ id: number; displayName: string }>> {
    const res = await this.ctx.get(this.url(PORTS.account, `/account/`));
    if (!res.ok()) throw new Error(`GET /account/ -> ${res.status()}`);
    return (await res.json()) as Array<{ id: number; displayName: string }>;
  }

  /** Raw POST to trade-service. Used to exercise server-side rejection paths. */
  async postTrade(body: {
    security: string;
    quantity: number;
    side: Side;
    accountId: number;
  }): Promise<{ status: number }> {
    const res = await this.ctx.post(this.url(PORTS.trade, `/trade/`), { data: body });
    return { status: res.status() };
  }
}
