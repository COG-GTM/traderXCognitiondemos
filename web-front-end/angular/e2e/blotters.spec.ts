import { expect, test } from '@playwright/test';
import {
  fillQuantity,
  openTicket,
  openTradeScreen,
  pickSecurity,
  positionBlotterRows,
  selectFirstAccount,
  tradeBlotterRows
} from './helpers';

test.describe('Blotters', () => {
  test.beforeEach(async ({ page }) => {
    await openTradeScreen(page);
  });

  // E2E-08 blotters render before an account is chosen
  test('renders both blotters empty before an account is selected', async ({ page }) => {
    await expect(tradeBlotterRows(page)).toHaveCount(0);
    await expect(positionBlotterRows(page)).toHaveCount(0);
  });

  // E2E-09 account switching must not leak rows between accounts
  test('replaces blotter content when the account is switched', async ({ page }) => {
    const dropdown = page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle');

    await dropdown.click();
    const options = page.locator('app-ngx-dropdown[name="account"] .dropdown-item');
    const count = await options.count();
    test.skip(count < 2, 'needs at least two accounts in the reference data');

    const firstName = ((await options.nth(0).textContent()) ?? '').trim();
    await options.nth(0).click();
    await expect(dropdown).toContainText(firstName);
    const firstRows = await tradeBlotterRows(page).allTextContents();

    await dropdown.click();
    const secondName = ((await page.locator('app-ngx-dropdown[name="account"] .dropdown-item').nth(1).textContent()) ?? '').trim();
    await page.locator('app-ngx-dropdown[name="account"] .dropdown-item').nth(1).click();
    await expect(dropdown).toContainText(secondName);

    await expect
      .poll(async () => (await tradeBlotterRows(page).allTextContents()).join('|'))
      .not.toEqual(firstRows.join('|'));
  });

  // E2E-10 a position appears/updates after a trade is booked
  test('updates the position blotter after a buy is booked', async ({ page }) => {
    await selectFirstAccount(page);

    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '4');
    await page.locator('#createButton').click();

    await expect(positionBlotterRows(page).first()).toBeVisible({ timeout: 20_000 });
  });
});
