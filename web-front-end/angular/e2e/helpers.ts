import { expect, Page } from '@playwright/test';

export const TRADE_ROUTE = '/trade';
export const ACCOUNT_ROUTE = '/account';

/** Opens the trade screen and waits for the two blotters to be rendered. */
export async function openTradeScreen(page: Page) {
  await page.goto(TRADE_ROUTE);
  await expect(page.locator('app-trade-blotter .ag-root-wrapper')).toBeVisible();
  await expect(page.locator('app-position-blotter .ag-root-wrapper')).toBeVisible();
}

/** Picks an account from the ngx-bootstrap dropdown by its display name. */
export async function selectAccount(page: Page, displayName: string) {
  await page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle').click();
  await page.locator('app-ngx-dropdown[name="account"] .dropdown-item', { hasText: displayName }).first().click();
  await expect(page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle')).toContainText(displayName);
}

/** Picks the first account in the dropdown and returns its display name. */
export async function selectFirstAccount(page: Page): Promise<string> {
  await page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle').click();
  const first = page.locator('app-ngx-dropdown[name="account"] .dropdown-item').first();
  const displayName = ((await first.textContent()) ?? '').trim();
  await first.click();
  await expect(page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle')).toContainText(displayName);
  return displayName;
}

export async function openTicket(page: Page) {
  await page.locator('#createTicketBtn').click();
  await expect(page.locator('#createButton')).toBeVisible();
}

/** Types into the security typeahead and selects the first suggestion. */
export async function pickSecurity(page: Page, query: string) {
  await page.locator('#stock-input').fill(query);
  const option = page.locator('typeahead-container .dropdown-item').first();
  await expect(option).toBeVisible();
  await option.click();
}

export async function fillQuantity(page: Page, quantity: string) {
  await page.locator('#quantityField').fill(quantity);
}

export function tradeBlotterRows(page: Page) {
  return page.locator('app-trade-blotter .ag-center-cols-container .ag-row');
}

export function positionBlotterRows(page: Page) {
  return page.locator('app-position-blotter .ag-center-cols-container .ag-row');
}
