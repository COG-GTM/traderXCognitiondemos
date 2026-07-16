import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config for the TraderX end-to-end browser suite.
 *
 * The suite runs against the full polyglot stack served through the nginx
 * ingress at http://localhost:8080 (see ../docker-compose.yml). Bring the stack
 * up first (`docker compose up --build` from the repo root) or use the
 * `npm run test:e2e` script which boots it and waits for readiness.
 *
 * Specs share a single, file-backed H2 database, so they run serially in a
 * single worker and assert values relative to the pre-trade baseline read over
 * the API (never hard-coded absolutes), which keeps the suite re-runnable
 * without a database reset.
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: require.resolve('./fixtures/readiness'),
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  timeout: 60_000,
  expect: { timeout: 20_000 },
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'results.xml' }],
  ],
  use: {
    baseURL: process.env.TRADERX_BASE_URL ?? 'http://localhost:8080',
    trace: 'on',
    screenshot: 'on',
    video: 'on',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
