import { request } from '@playwright/test';

/**
 * Global setup: poll deep readiness of the full stack before any spec runs.
 *
 * The ingress returns 502 until the Angular dev server finishes its first
 * bundle, so we never gate on a fixed timer — we poll the ingress and the key
 * backend APIs until they all answer, or fail fast after the timeout.
 */

const HOST = process.env.TRADERX_HOST ?? 'localhost';
const BASE = process.env.TRADERX_BASE_URL ?? `http://${HOST}:8080`;
const TIMEOUT_MS = Number(process.env.TRADERX_READY_TIMEOUT ?? 600_000);
const INTERVAL_MS = 3_000;

const CHECKS: Array<{ name: string; url: string }> = [
  { name: 'ingress SPA', url: `${BASE}/` },
  { name: 'account-service', url: `${BASE}/account-service/account/` },
  { name: 'reference-data', url: `${BASE}/reference-data/stocks/IBM` },
];

async function waitFor(name: string, url: string, deadline: number): Promise<void> {
  const ctx = await request.newContext();
  try {
    let lastStatus = 0;
    let lastErr = '';
    while (Date.now() < deadline) {
      try {
        const res = await ctx.get(url, { timeout: 10_000 });
        lastStatus = res.status();
        if (res.ok()) {
          console.log(`[readiness] ${name} ready (${lastStatus})`);
          return;
        }
      } catch (e) {
        lastErr = (e as Error).message;
      }
      await new Promise((r) => setTimeout(r, INTERVAL_MS));
    }
    throw new Error(
      `[readiness] ${name} at ${url} not ready before timeout (last status ${lastStatus}${lastErr ? `, err: ${lastErr}` : ''})`,
    );
  } finally {
    await ctx.dispose();
  }
}

export default async function globalSetup(): Promise<void> {
  const deadline = Date.now() + TIMEOUT_MS;
  console.log(`[readiness] waiting for TraderX stack at ${BASE} ...`);
  for (const c of CHECKS) {
    await waitFor(c.name, c.url, deadline);
  }
  console.log('[readiness] full stack is ready.');
}
