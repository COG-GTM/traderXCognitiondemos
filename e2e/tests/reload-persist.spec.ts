import { test, expect, ACCOUNT, TICKERS, waitForNewTrade } from '../fixtures/app';

/**
 * §2.4 Reload persists.
 *
 * Place a BUY, reload the page, re-select the account, and assert the trade +
 * position are still present. After a reload these load over REST
 * (GET /trades/{id}, GET /positions/{id}) before any socket updates, proving
 * server-side persistence rather than just live socket state.
 *
 * Uses security C (seeded C -2000 on 22214) to stay disjoint from the BUY/SELL
 * specs so the suite is order-independent.
 */
test('a BUY persists across a page reload', async ({ page, tradePage, api }) => {
  const security = TICKERS.C.ticker;
  const qty = 100;

  const baselineQty = await api.getPositionQuantity(ACCOUNT.id, security);
  const knownIds = new Set((await api.getTrades(ACCOUNT.id)).map((t) => t.id));

  await tradePage.goto();
  await tradePage.selectAccount(ACCOUNT.displayName);
  await tradePage.openTicket();
  await tradePage.fillTicket({ companyName: TICKERS.C.companyName, side: 'Buy', quantity: qty });
  await tradePage.submitTicket();

  const tradeId = await waitForNewTrade(api, ACCOUNT.id, knownIds, {
    security,
    side: 'Buy',
    quantity: qty,
  });
  const expectedQty = baselineQty + qty;
  await tradePage.expectPositionQuantity(security, expectedQty);

  // Reload and re-select the account: data must reload from the server.
  await page.reload();
  await expect(page.locator('#createTicketBtn')).toBeVisible();
  await tradePage.selectAccount(ACCOUNT.displayName);

  await tradePage.expectSettledTrade(tradeId, { security, quantity: qty, side: 'Buy' });
  await tradePage.expectPositionQuantity(security, expectedQty);

  // API/DB cross-check of persistence.
  expect(await api.getPositionQuantity(ACCOUNT.id, security)).toBe(expectedQty);
});
