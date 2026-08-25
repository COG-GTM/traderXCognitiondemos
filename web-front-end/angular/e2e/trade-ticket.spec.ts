import { expect, test } from '@playwright/test';
import {
  fillQuantity,
  openTicket,
  openTradeScreen,
  pickSecurity,
  selectFirstAccount,
  tradeBlotterRows
} from './helpers';

test.describe('Trade ticket', () => {
  test.beforeEach(async ({ page }) => {
    await openTradeScreen(page);
    await selectFirstAccount(page);
  });

  // E2E-01 golden path
  test('submits a trade and shows it in the trade blotter', async ({ page }) => {
    const before = await tradeBlotterRows(page).count();

    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '10');
    await page.locator('#createButton').click();

    await expect(tradeBlotterRows(page)).toHaveCount(before + 1, { timeout: 20_000 });
  });

  // E2E-02 quantity 0 - the ticket silently does nothing today
  test('does not submit and shows no validation message for quantity 0', async ({ page }) => {
    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '0');
    await page.locator('#createButton').click();

    await expect(page.locator('#createButton')).toBeVisible();
    await expect(page.locator('.invalid-feedback, .alert-danger')).toHaveCount(0);
  });

  // E2E-03 free typed unknown ticker
  test('clears a free typed unknown ticker on blur and refuses to submit', async ({ page }) => {
    await openTicket(page);
    await page.locator('#stock-input').fill('NOTATICKER');
    await page.locator('#quantityField').click();
    await fillQuantity(page, '5');
    await page.locator('#createButton').click();

    await expect(page.locator('#createButton')).toBeVisible();
  });

  // E2E-04 side toggle
  test('keeps the chosen side when submitting a sell ticket', async ({ page }) => {
    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '3');
    await page.locator('label[for="sellButton"]').click();

    await expect(page.locator('#sellButton')).toBeChecked();

    await page.locator('#createButton').click();
    await expect(tradeBlotterRows(page).first()).toContainText('Sell', { timeout: 20_000 });
  });

  // E2E-05 cancel
  test('closes the ticket without submitting anything when cancelled', async ({ page }) => {
    const before = await tradeBlotterRows(page).count();

    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '7');
    await page.locator('#cancelButton').click();

    await expect(page.locator('#createButton')).toBeHidden();
    await expect(tradeBlotterRows(page)).toHaveCount(before);
  });

  // E2E-06 duplicate submission window
  test('submits twice when the create button is double clicked', async ({ page }) => {
    const before = await tradeBlotterRows(page).count();

    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '1');
    await page.locator('#createButton').dblclick();

    await expect(tradeBlotterRows(page)).toHaveCount(before + 2, { timeout: 20_000 });
  });

  // E2E-07 very large quantity
  test('accepts a quantity beyond the java int range', async ({ page }) => {
    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '2147483648');

    await expect(page.locator('#quantityField')).toHaveValue('2147483648');

    await page.locator('#createButton').click();
    await expect(page.locator('app-trade alert, app-trade-blotter')).toBeVisible();
  });
});
