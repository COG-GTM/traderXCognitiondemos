import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { valuePortfolio, clearCache } from '../src/valuation/valuation.js';
import { normalizeQuote } from '../src/pricing/normalize.js';

const positions = JSON.parse(readFileSync('fixtures/positions.json', 'utf8'));

function quotesFrom(file) {
  const byIsin = {};
  for (const q of JSON.parse(readFileSync(file, 'utf8')).quotes) byIsin[q.isin] = q;
  return byIsin;
}

test('values a portfolio from an ArdenFeed v2.2 batch', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.2.json'));
  assert.equal(rows[0].marketValue, '148080.00');
  assert.equal(rows[0].ccy, 'GBP');
});

test('values a portfolio from an ArdenFeed v2.3 batch (px is an object) — INC-2026-0925-01', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
  assert.equal(rows[0].price, '12.4100');
  assert.equal(rows[0].ccy, 'GBP');
  assert.equal(rows[0].marketValue, '148920.00');
});

test('v2.2 and v2.3 batches for the same quote normalise to the same shape', () => {
  const a = normalizeQuote({ isin: 'X', px: 1.5, ccy: 'EUR', asof: 't' });
  const b = normalizeQuote({ isin: 'X', px: { v: 1.5, ccy: 'EUR' }, asof: 't' });
  assert.deepEqual(a, b);
});

test('rejects an unknown quote shape loudly instead of producing NaN valuations', () => {
  assert.throws(() => normalizeQuote({ isin: 'X', px: '1.5', asof: 't' }), TypeError);
});
