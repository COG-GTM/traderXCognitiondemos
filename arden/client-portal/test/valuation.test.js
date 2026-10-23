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

// INC-2026-0925-01: ArdenFeed v2.3 (effective 2026-09-25 06:00Z) sends px as { v, ccy }.
test('values the live ArdenFeed v2.3 batch from 2026-09-25 06:00Z', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
  assert.deepEqual(rows.map((r) => [r.isin, r.price, r.ccy, r.marketValue]), [
    ['GB0002634946', '12.4100', 'GBP', '148920.00'],
    ['CH0012032048', '270.9000', 'CHF', '216720.00'],
    ['US0378331005', '230.4000', 'USD', '345600.00'],
  ]);
});

test('a v2.2 quote and the same quote in v2.3 shape value identically', () => {
  const v22 = { isin: 'CH0012032048', px: 271.5, ccy: 'CHF', asof: '2026-09-24T16:35:00Z' };
  const v23 = { isin: 'CH0012032048', px: { v: 271.5, ccy: 'CHF' }, asof: '2026-09-24T16:35:00Z' };
  assert.deepEqual(normalizeQuote(v23), normalizeQuote(v22));
  clearCache();
  const a = valuePortfolio(positions.slice(1, 2), { CH0012032048: v22 });
  clearCache();
  const b = valuePortfolio(positions.slice(1, 2), { CH0012032048: v23 });
  assert.deepEqual(b, a);
});

test('rejects quote shapes that are neither v2.2 nor v2.3 instead of guessing', () => {
  const base = { isin: 'GB0002634946', asof: '2026-09-25T06:00:00Z' };
  const bad = [
    { ...base, px: '12.41', ccy: 'GBP' },
    { ...base, px: 12.41 },
    { ...base, px: Number.NaN, ccy: 'GBP' },
    { ...base, px: { v: '12.41', ccy: 'GBP' } },
    { ...base, px: { value: 12.41, ccy: 'GBP' } },
    { ...base, px: { v: 12.41 } },
    { ...base, px: { v: 12.41, ccy: 'GBP' }, ccy: 'USD' },
    { ...base, px: null, ccy: 'GBP' },
  ];
  for (const q of bad) {
    assert.throws(() => normalizeQuote(q), /unsupported ArdenFeed quote shape/, JSON.stringify(q));
    clearCache();
    assert.throws(() => valuePortfolio(positions.slice(0, 1), { GB0002634946: q }), /unsupported ArdenFeed quote shape/);
  }
});
