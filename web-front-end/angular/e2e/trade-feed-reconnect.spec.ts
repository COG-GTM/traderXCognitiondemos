import { expect, test } from '@playwright/test';
import {
  fillQuantity,
  openTicket,
  openTradeScreen,
  pickSecurity,
  selectFirstAccount,
  tradeBlotterRows
} from './helpers';

/**
 * Socket resilience: the trade feed is a socket.io connection created once by
 * TradeFeedService. Dropping it should not break the page, and a trade booked
 * afterwards should still make its way into the blotter once the client
 * reconnects (socket.io reconnects automatically).
 */
test.describe('Trade feed socket drop', () => {
  test('keeps the UI usable and recovers after the socket is dropped', async ({ page }) => {
    await page.addInitScript(() => {
      const opened: WebSocket[] = [];
      (window as unknown as { __traderxSockets: WebSocket[] }).__traderxSockets = opened;
      const NativeWebSocket = window.WebSocket;
      class TrackedWebSocket extends NativeWebSocket {
        constructor(url: string | URL, protocols?: string | string[]) {
          super(url, protocols);
          opened.push(this);
        }
      }
      window.WebSocket = TrackedWebSocket as unknown as typeof WebSocket;
    });

    await openTradeScreen(page);
    await selectFirstAccount(page);

    const before = await tradeBlotterRows(page).count();

    // Force every live websocket in the page to close, simulating a feed outage.
    await page.evaluate(() => {
      const sockets = (window as unknown as { __traderxSockets?: WebSocket[] }).__traderxSockets ?? [];
      sockets.forEach((socket) => socket.close());
    });

    await expect(page.locator('app-trade-blotter .ag-root-wrapper')).toBeVisible();

    await openTicket(page);
    await pickSecurity(page, 'A');
    await fillQuantity(page, '2');
    await page.locator('#createButton').click();

    await expect(tradeBlotterRows(page)).toHaveCount(before + 1, { timeout: 30_000 });
  });

  test('recovers the blotter contents after a full page reload', async ({ page }) => {
    await openTradeScreen(page);
    const account = await selectFirstAccount(page);

    await page.reload();
    await expect(page.locator('app-trade-blotter .ag-root-wrapper')).toBeVisible();
    await expect(page.locator('app-ngx-dropdown[name="account"] button.dropdown-toggle')).not.toContainText(account);
  });
});
