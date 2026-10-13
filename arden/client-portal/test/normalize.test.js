import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { normalizeQuote } from '../src/pricing/normalize.js';
import { valuePortfolio, clearCache } from '../src/valuation/valuation.js';

const positions = JSON.parse(readFileSync('fixtures/positions.json', 'utf8'));
const quotesFrom = (file) => Object.fromEntries(JSON.parse(readFileSync(file, 'utf8')).quotes.map((q) => [q.isin, q]));

test('ARD-f47f5f regression: values a portfolio from the live ArdenFeed v2.3 batch', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
  assert.deepEqual(rows.map((r) => [r.isin, r.price, r.ccy, r.marketValue]), [
    ['GB0002634946', '12.4100', 'GBP', '148920.00'],
    ['CH0012032048', '270.9000', 'CHF', '216720.00'],
    ['US0378331005', '230.4000', 'USD', '345600.00'],
  ]);
});

test('v2.2 and v2.3 quotes normalise identically', () => {
  const asof = '2026-09-25T06:00:00Z';
  assert.deepEqual(
    normalizeQuote({ isin: 'GB0002634946', px: 12.41, ccy: 'GBP', asof }),
    normalizeQuote({ isin: 'GB0002634946', px: { v: 12.41, ccy: 'GBP' }, asof }),
  );
});

test('v2.3 px.ccy takes precedence over the deprecated quote-level ccy', () => {
  const n = normalizeQuote({ isin: 'GB0002634946', px: { v: 12.41, ccy: 'GBP' }, ccy: 'USD', asof: '2026-09-25T06:00:00Z' });
  assert.equal(n.px, 12.41);
  assert.equal(n.ccy, 'GBP');
});

test('unknown quote shapes fail loudly instead of producing a number', () => {
  const asof = '2026-09-25T06:00:00Z';
  for (const q of [
    { isin: 'X', px: '12.41', ccy: 'GBP', asof },
    { isin: 'X', px: { value: 12.41, ccy: 'GBP' }, asof },
    { isin: 'X', px: { v: 12.41 }, asof },
    { isin: 'X', px: 12.41, asof },
    undefined,
  ]) {
    assert.throws(() => normalizeQuote(q), TypeError);
  }
});
