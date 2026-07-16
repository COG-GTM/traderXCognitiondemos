import { test, expect, ACCOUNT, TICKERS, waitForNewTrade } from '../fixtures/app';

/**
 * §2.1 Primary golden path: place a BUY.
 *
 * On the deterministic seed, account 22214 holds IBM -100; buying 100 offsets
 * the short to 0. The fill is asynchronous (New -> Processing -> Settled and the
 * position streams back over the trade-feed socket), so every assertion
 * auto-retries on final UI state.
 */
test('BUY 100 IBM on 22214 settles, updates the blotter and position', async ({
  tradePage,
  api,
}) => {
  const security = TICKERS.IBM.ticker;
  const qty = 100;

  const baselineQty = await api.getPositionQuantity(ACCOUNT.id, security);
  const knownIds = new Set((await api.getTrades(ACCOUNT.id)).map((t) => t.id));

  await tradePage.goto();
  await tradePage.selectAccount(ACCOUNT.displayName);
  await tradePage.expectPositionQuantity(security, baselineQty); // precondition (seed: -100)

  await tradePage.openTicket();
  await tradePage.fillTicket({ companyName: TICKERS.IBM.companyName, side: 'Buy', quantity: qty });
  await tradePage.submitTicket();

  // Identify the exact new trade row via the API, then assert the UI row settles.
  const tradeId = await waitForNewTrade(api, ACCOUNT.id, knownIds, {
    security,
    side: 'Buy',
    quantity: qty,
  });
  await tradePage.expectSettledTrade(tradeId, { security, quantity: qty, side: 'Buy' });

  // Position moves baseline -> baseline + 100 (seed: -100 -> 0).
  const expectedQty = baselineQty + qty;
  await tradePage.expectPositionQuantity(security, expectedQty);

  // API/DB cross-check.
  expect(await api.getPositionQuantity(ACCOUNT.id, security)).toBe(expectedQty);
  const persisted = (await api.getTrades(ACCOUNT.id)).find((t) => t.id === tradeId);
  expect(persisted).toBeTruthy();
  expect(persisted!.state).toBe('Settled');
});
