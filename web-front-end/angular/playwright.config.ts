import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end configuration for the TraderX web front end.
 *
 * These specs drive the real UI against a running stack (see e2e/README notes in
 * the project README) and are deliberately NOT part of the CI pipeline.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.TRADERX_URL ?? 'http://localhost:18093',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
