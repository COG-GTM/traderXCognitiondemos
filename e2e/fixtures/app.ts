import { test as base } from '@playwright/test';
import { TradePage } from '../pages/trade.page';
import { TraderXApi } from '../utils/api';

/** Deterministic seed data the specs rely on (see database/initialSchema.sql). */
export const ACCOUNT = { id: 22214, displayName: 'Test Account 20' } as const;

export const TICKERS = {
  IBM: { ticker: 'IBM', companyName: 'IBM' },
  MS: { ticker: 'MS', companyName: 'Morgan Stanley' },
  C: { ticker: 'C', companyName: 'Citigroup' },
  BAC: { ticker: 'BAC', companyName: 'Bank of America' },
} as const;

type Fixtures = {
  tradePage: TradePage;
  api: TraderXApi;
};

export const test = base.extend<Fixtures>({
  tradePage: async ({ page }, use) => {
    await use(new TradePage(page));
  },
  api: async ({}, use) => {
    const api = await TraderXApi.create();
    await use(api);
    await api.dispose();
  },
});

export const expect = test.expect;

/**
 * Poll the trades API until a newly-created trade (not present in `knownIds`)
 * matching the given attributes appears, and return its id. Ties the UI action
 * to server-side persistence and lets specs target the exact ag-Grid row.
 */
export async function waitForNewTrade(
  api: TraderXApi,
  accountId: number,
  knownIds: Set<string>,
  match: { security: string; side: 'Buy' | 'Sell'; quantity: number },
): Promise<string> {
  const deadline = Date.now() + 30_000;
  let last: string[] = [];
  while (Date.now() < deadline) {
    const trades = await api.getTrades(accountId);
    const found = trades.find(
      (t) =>
        !knownIds.has(t.id) &&
        t.security === match.security &&
        t.side === match.side &&
        t.quantity === match.quantity,
    );
    if (found) return found.id;
    last = trades.map((t) => `${t.id}:${t.security}/${t.side}/${t.quantity}/${t.state}`);
    await new Promise((r) => setTimeout(r, 1_000));
  }
  throw new Error(
    `New ${match.side} ${match.quantity} ${match.security} trade did not appear. Current trades: ${last.join(', ')}`,
  );
}
