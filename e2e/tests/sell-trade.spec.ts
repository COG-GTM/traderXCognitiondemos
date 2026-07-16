import { test, expect, ACCOUNT, TICKERS, waitForNewTrade } from '../fixtures/app';

/**
 * §2.2 Place a SELL.
 *
 * Account 22214 holds MS +1000 on the seed; selling 100 reduces it to 900.
 */
test('SELL 100 MS on 22214 settles, updates the blotter and position', async ({
  tradePage,
  api,
}) => {
  const security = TICKERS.MS.ticker;
  const qty = 100;

  const baselineQty = await api.getPositionQuantity(ACCOUNT.id, security);
  const knownIds = new Set((await api.getTrades(ACCOUNT.id)).map((t) => t.id));

  await tradePage.goto();
  await tradePage.selectAccount(ACCOUNT.displayName);
  await tradePage.expectPositionQuantity(security, baselineQty); // precondition (seed: 1000)

  await tradePage.openTicket();
  await tradePage.fillTicket({ companyName: TICKERS.MS.companyName, side: 'Sell', quantity: qty });
  await tradePage.submitTicket();

  const tradeId = await waitForNewTrade(api, ACCOUNT.id, knownIds, {
    security,
    side: 'Sell',
    quantity: qty,
  });
  await tradePage.expectSettledTrade(tradeId, { security, quantity: qty, side: 'Sell' });

  // Position moves baseline -> baseline - 100 (seed: 1000 -> 900).
  const expectedQty = baselineQty - qty;
  await tradePage.expectPositionQuantity(security, expectedQty);

  // API/DB cross-check.
  expect(await api.getPositionQuantity(ACCOUNT.id, security)).toBe(expectedQty);
  const persisted = (await api.getTrades(ACCOUNT.id)).find((t) => t.id === tradeId);
  expect(persisted).toBeTruthy();
  expect(persisted!.state).toBe('Settled');
});
