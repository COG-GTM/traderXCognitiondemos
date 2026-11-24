import { Page, TestInfo, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export const runId = () => `${Date.now().toString(36)}${Math.floor(Math.random() * 1e4)}`;

// Both apps use the same path routes (no hash routing), so a logical route maps 1:1.
export const route = (logical: string) => logical;

export async function gotoApp(page: Page, logical: string) {
  await page.goto(route(logical));
  await expect(page.getByText('FINOS | TraderX Sample Application')).toBeVisible();
}

// Full-page screenshot saved as report/screens/<project>/<name>.png for the side-by-side report.
export async function snap(page: Page, testInfo: TestInfo, name: string) {
  const dir = path.join(__dirname, '..', 'report', 'screens', testInfo.project.name);
  fs.mkdirSync(dir, { recursive: true });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(dir, `${name}.png`), fullPage: true, animations: 'disabled' });
}

export const accountApi = 'http://localhost:18088';

export async function createAccountViaApi(page: Page, displayName: string): Promise<number> {
  const res = await page.request.post(`${accountApi}/account/`, { data: { displayName } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).id;
}

export function grid(page: Page, idOrHeading: string) {
  return page.locator(idOrHeading);
}
