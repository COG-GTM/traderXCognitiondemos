import { test, expect, ACCOUNT, TICKERS } from '../fixtures/app';
import { PORTS } from '../utils/api';

/**
 * §2.3 Invalid / rejected orders across two layers.
 */
test.describe('invalid orders', () => {
  test('UI guard: quantity 0 with a valid ticker submits nothing', async ({ tradePage, api }) => {
    const before = (await api.getTrades(ACCOUNT.id)).length;

    await tradePage.goto();
    await tradePage.selectAccount(ACCOUNT.displayName);
    await tradePage.openTicket();
    await tradePage.fillTicket({ companyName: TICKERS.IBM.companyName, side: 'Buy', quantity: 0 });
    await tradePage.submitTicket();

    // onCreate() returns early: no success alert, no new trade persisted.
    await expect(tradePage.successAlert()).toHaveCount(0);
    await expect
      .poll(async () => (await api.getTrades(ACCOUNT.id)).length, { timeout: 5_000 })
      .toBe(before);
  });

  test('UI guard: typed but unselected ticker submits nothing', async ({ tradePage, api }) => {
    const before = (await api.getTrades(ACCOUNT.id)).length;

    await tradePage.goto();
    await tradePage.selectAccount(ACCOUNT.displayName);
    await tradePage.openTicket();
    // Type a bogus symbol and never pick a suggestion -> onBlur clears security.
    await tradePage.fillTicket({
      companyName: 'ZZZZ',
      side: 'Buy',
      quantity: 50,
      selectSuggestion: false,
    });
    await tradePage.submitTicket();

    await expect(tradePage.successAlert()).toHaveCount(0);
    await expect
      .poll(async () => (await api.getTrades(ACCOUNT.id)).length, { timeout: 5_000 })
      .toBe(before);
  });

  test('server rejection: POST with an unknown ticker returns 404', async ({ api }) => {
    const res = await api.postTrade({
      security: 'ZZZZ',
      quantity: 100,
      side: 'Buy',
      accountId: ACCOUNT.id,
    });
    expect(res.status).toBe(404);
    expect(PORTS.trade).toBe(18092);
  });
});
