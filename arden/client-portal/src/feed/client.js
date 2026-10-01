import { readFileSync } from 'node:fs';

// In production this hits ArdenFeed's REST endpoint; locally it reads a batch file so demos are deterministic.
export function loadQuotes(path = process.env.ARDENFEED_BATCH ?? 'fixtures/ardenfeed-v2.2.json') {
  const batch = JSON.parse(readFileSync(path, 'utf8'));
  const byIsin = {};
  for (const q of batch.quotes) byIsin[q.isin] = q;
  return byIsin;
}
