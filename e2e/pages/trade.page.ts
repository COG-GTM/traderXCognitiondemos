import { expect, Locator, Page } from '@playwright/test';

export type Side = 'Buy' | 'Sell';

/**
 * Page Object for the TraderX Trade view: account dropdown, the Create Trade
 * Ticket modal, and the Trade / Position ag-Grid blotters.
 *
 * All lookups use stable element ids from the Angular templates, role/text for
 * the ngx-bootstrap dropdown + typeahead, and ag-Grid's `row-id` / `col-id`
 * attributes for blotter rows and cells.
 */
export class TradePage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/');
    await expect(this.page.locator('#createTicketBtn')).toBeVisible();
  }

  private get accountToggle(): Locator {
    return this.page.locator('app-ngx-dropdown button.dropdown-toggle');
  }

  /** Select an account by its display name (e.g. "Test Account 20" for 22214). */
  async selectAccount(displayName: string): Promise<void> {
    await this.accountToggle.click();
    await this.page
      .locator('.dropdown-menu a.dropdown-item', { hasText: displayName })
      .first()
      .click();
    await expect(this.accountToggle).toContainText(displayName);
  }

  async openTicket(): Promise<void> {
    await this.page.locator('#createTicketBtn').click();
    await expect(this.page.locator('#createButton')).toBeVisible();
  }

  async closeTicket(): Promise<void> {
    await this.page.locator('#cancelButton').click();
  }

  /**
   * Fill the ticket. `companyName` must match a reference-data companyName
   * (e.g. "IBM", "Morgan Stanley", "Citigroup") — the security is only set when
   * a typeahead suggestion is clicked.
   */
  async fillTicket(opts: {
    companyName: string;
    side: Side;
    quantity: number;
    selectSuggestion?: boolean;
  }): Promise<void> {
    const { companyName, side, quantity, selectSuggestion = true } = opts;
    const stockInput = this.page.locator('#stock-input');
    await stockInput.click();
    await stockInput.fill(companyName);
    if (selectSuggestion) {
      await this.page
        .locator('typeahead-container button.dropdown-item', { hasText: companyName })
        .first()
        .click();
    }
    if (side === 'Sell') {
      await this.page.locator('label[for="sellButton"]').click();
    } else {
      await this.page.locator('label[for="buyButton"]').click();
    }
    await this.page.locator('#quantityField').fill(String(quantity));
  }

  async submitTicket(): Promise<void> {
    await this.page.locator('#createButton').click();
  }

  successAlert(): Locator {
    return this.page.locator('alert .alert-success');
  }

  // --- Trade blotter -------------------------------------------------------

  tradeRow(tradeId: string): Locator {
    return this.page.locator(`app-trade-blotter .ag-row[row-id="Trade-${tradeId}"]`);
  }

  tradeCell(tradeId: string, colId: 'security' | 'quantity' | 'side' | 'state'): Locator {
    return this.tradeRow(tradeId).locator(`[col-id="${colId}"]`);
  }

  /** Assert a trade row exists and settles to the expected values (auto-retry). */
  async expectSettledTrade(
    tradeId: string,
    expected: { security: string; quantity: number; side: Side },
  ): Promise<void> {
    await expect(this.tradeRow(tradeId)).toBeVisible();
    await expect(this.tradeCell(tradeId, 'security')).toHaveText(expected.security);
    await expect(this.tradeCell(tradeId, 'quantity')).toHaveText(String(expected.quantity));
    await expect(this.tradeCell(tradeId, 'side')).toHaveText(expected.side);
    await expect(this.tradeCell(tradeId, 'state')).toHaveText('Settled');
  }

  // --- Position blotter ----------------------------------------------------

  positionRow(security: string): Locator {
    return this.page.locator(`app-position-blotter .ag-row[row-id="Position-${security}"]`);
  }

  positionQuantityCell(security: string): Locator {
    return this.positionRow(security).locator('[col-id="quantity"]');
  }

  /** Assert a position row settles to the expected quantity (auto-retry). */
  async expectPositionQuantity(security: string, quantity: number): Promise<void> {
    await expect(this.positionRow(security)).toBeVisible();
    await expect(this.positionQuantityCell(security)).toHaveText(String(quantity));
  }

  tradeRowCount(): Locator {
    return this.page.locator('app-trade-blotter .ag-center-cols-container .ag-row');
  }
}
