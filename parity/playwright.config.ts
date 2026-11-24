import { defineConfig, devices } from '@playwright/test';

// Same specs run against both apps. Both talk to the same local TraderX backend
// (ports 18082-18092), so tests run serially and create their own timestamped data.
export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  retries: 1,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['json', { outputFile: 'report/results.json' }]],
  use: {
    viewport: { width: 1400, height: 1000 },
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  projects: [
    { name: 'baseline', use: { baseURL: process.env.BASELINE_URL ?? 'http://localhost:4000' } },
    { name: 'upgraded', use: { baseURL: process.env.UPGRADED_URL ?? 'http://localhost:4200' } },
  ],
});
