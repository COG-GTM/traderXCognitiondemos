import { test, expect, ACCOUNT } from '../fixtures/app';

test.describe('smoke: stack bring-up', () => {
  test('loads the Trade view, lists accounts, and renders seeded blotters', async ({
    page,
    tradePage,
    api,
  }) => {
    await tradePage.goto();

    // Header + primary controls render.
    await expect(page.getByRole('link', { name: 'Trade' })).toBeVisible();
    await expect(page.locator('#createTicketBtn')).toBeVisible();

    // Account dropdown lists the 7 seeded accounts.
    await page.locator('app-ngx-dropdown button.dropdown-toggle').click();
    await expect(page.locator('.dropdown-menu a.dropdown-item')).toHaveCount(7);
    await page.locator('.dropdown-menu a.dropdown-item', { hasText: ACCOUNT.displayName }).click();

    // Seeded blotters render for account 22214.
    await tradePage.expectPositionQuantity('MS', await api.getPositionQuantity(ACCOUNT.id, 'MS'));
    await expect(tradePage.tradeRowCount().first()).toBeVisible();

    // API sanity: 7 accounts served.
    const accounts = await api.getAccounts();
    expect(accounts.length).toBe(7);
  });
});
