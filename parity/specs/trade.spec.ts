import { test, expect } from '@playwright/test';
import { gotoApp, snap, runId, createAccountViaApi } from './helpers';

test.describe('Trade page', () => {
  test('default route redirects to /trade and shows blotters for the default account', async ({ page }, testInfo) => {
    await gotoApp(page, '/');
    await expect(page).toHaveURL(/\/trade$/);
    await expect(page.getByRole('link', { name: 'Trade' })).toHaveClass(/active/);
    await expect(page.getByRole('button', { name: 'Big Corporate Fund' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Trades' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Positions' })).toBeVisible();
    for (const col of ['SECURITY', 'QUANTITY', 'SIDE', 'STATE']) {
      await expect(page.locator('app-trade-blotter .ag-header-cell-text', { hasText: col })).toBeVisible();
    }
    await expect(page.locator('app-position-blotter .ag-header-cell-text', { hasText: 'SECURITY' })).toBeVisible();
    await expect(page.locator('app-trade-blotter .ag-center-cols-container .ag-row').first()).toBeVisible();
    await snap(page, testInfo, '01-trade');
  });

  test('account dropdown lists accounts and switching reloads the blotters', async ({ page }, testInfo) => {
    await gotoApp(page, '/trade');
    await page.getByRole('button', { name: 'Big Corporate Fund' }).click();
    const menu = page.getByRole('menu');
    await expect(menu.getByRole('menuitem', { name: 'Internal Trading Book' })).toBeVisible();
    await expect(menu.getByRole('menuitem', { name: 'Big Corporate Fund' }).locator('a')).toHaveClass(/active/);
    await snap(page, testInfo, '02-trade-account-dropdown');
    const trades = page.locator('app-trade-blotter .ag-center-cols-container');
    const positions = page.locator('app-position-blotter .ag-center-cols-container');
    await expect(trades.getByText('BAC', { exact: true })).toBeVisible();
    await expect(positions.getByText('BAC', { exact: true })).toBeVisible();
    await menu.getByRole('menuitem', { name: 'Test Account 20' }).click();
    await expect(page.getByRole('button', { name: 'Test Account 20' })).toBeVisible();
    await expect(trades.getByText('IBM', { exact: true })).toBeVisible();
    await expect(positions.getByText('IBM', { exact: true })).toBeVisible();
    await expect(trades.getByText('BAC', { exact: true })).toHaveCount(0);
    await expect(positions.getByText('BAC', { exact: true })).toHaveCount(0);
  });

  test('trade ticket validates input, then books a trade that settles and updates positions', async ({ page }, testInfo) => {
    const name = `Parity ${testInfo.project.name} ${runId()}`;
    await createAccountViaApi(page, name);
    await gotoApp(page, '/trade');
    await page.getByRole('button', { name: 'Big Corporate Fund' }).click();
    await page.getByRole('menuitem', { name }).click();
    await expect(page.getByRole('button', { name })).toBeVisible();
    await expect(page.locator('app-trade-blotter').getByText('No Rows To Show')).toBeVisible();

    await page.getByRole('button', { name: 'Create Trade Ticket' }).click();
    const modal = page.locator('.modal-content');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#accountLabel')).toHaveValue(name);
    await expect(modal.locator('#accountLabel')).toBeDisabled();
    await expect(modal.locator('#buyButton')).toBeChecked();
    await snap(page, testInfo, '03-trade-ticket');

    // Validation: no security / quantity -> Create does nothing, modal stays open
    await modal.getByRole('button', { name: 'Create' }).click();
    await expect(modal).toBeVisible();

    await modal.locator('#stock-input').fill('Micro');
    await expect(page.locator('typeahead-container').getByText('Microsoft')).toBeVisible();
    await snap(page, testInfo, '04-trade-ticket-typeahead');
    await page.locator('typeahead-container').getByText('Microsoft').click();
    await expect(modal.locator('#stock-input')).toHaveValue('Microsoft');
    // still invalid without a quantity
    await modal.getByRole('button', { name: 'Create' }).click();
    await expect(modal).toBeVisible();

    await modal.locator('#quantityField').fill('150');
    await modal.getByRole('button', { name: 'Create' }).click();
    await expect(modal).toBeHidden();
    // trade-service echoes the order; the UI renders it as JSON in an alert
    await expect(page.locator('app-trade alert')).toContainText('"security": "MSFT"');
    await expect(page.locator('app-trade alert')).toContainText('"quantity": 150');

    const tradeRow = page.locator('app-trade-blotter .ag-center-cols-container .ag-row').first();
    await expect(tradeRow).toContainText('MSFT');
    await expect(tradeRow).toContainText('150');
    await expect(tradeRow).toContainText('Buy');
    await expect(tradeRow).toContainText('Settled', { timeout: 20_000 });
    const posRow = page.locator('app-position-blotter .ag-center-cols-container .ag-row').first();
    await expect(posRow).toContainText('MSFT', { timeout: 20_000 });
    await expect(posRow).toContainText('150');
  });

  test('trade ticket Close button dismisses without booking', async ({ page }) => {
    const name = `Parity close ${runId()}`;
    await createAccountViaApi(page, name);
    await gotoApp(page, '/trade');
    await page.getByRole('button', { name: 'Big Corporate Fund' }).click();
    await page.getByRole('menuitem', { name }).click();
    await page.getByRole('button', { name: 'Create Trade Ticket' }).click();
    const modal = page.locator('.modal-content');
    await modal.locator('label[for="sellButton"]').click();
    await expect(modal.locator('#sellButton')).toBeChecked();
    await modal.getByRole('button', { name: 'Close' }).click();
    await expect(modal).toBeHidden();
    await expect(page.locator('app-trade-blotter').getByText('No Rows To Show')).toBeVisible();
  });
});

test('unknown route shows Page not found', async ({ page }, testInfo) => {
  await gotoApp(page, '/does-not-exist');
  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible();
  await snap(page, testInfo, '07-not-found');
});
