import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { valuePortfolio, clearCache } from '../src/valuation/valuation.js';

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

test('INC-2026-0925-01: values the live ArdenFeed v2.3 batch (px object)', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
  assert.deepEqual(rows.map((r) => [r.isin, r.price, r.ccy, r.marketValue]), [
    ['GB0002634946', '12.4100', 'GBP', '148920.00'],
    ['CH0012032048', '270.9000', 'CHF', '216720.00'],
    ['US0378331005', '230.4000', 'USD', '345600.00'],
  ]);
});

test('v2.2 and v2.3 shapes of the same quote value identically', () => {
  const v22 = quotesFrom('fixtures/ardenfeed-v2.2.json');
  const v23 = {};
  for (const [isin, q] of Object.entries(v22)) v23[isin] = { isin, px: { v: q.px, ccy: q.ccy }, asof: q.asof };
  clearCache();
  const a = valuePortfolio(positions, v22);
  clearCache();
  const b = valuePortfolio(positions, v23);
  assert.deepEqual(b, a);
});

test('unknown or inconsistent quote shapes fail loudly instead of producing a number', () => {
  const p = [{ account: 'ACC-X', isin: 'GB0002634946', quantity: 10 }];
  const bad = [
    { px: '12.34', ccy: 'GBP' },
    { px: { value: 12.34, ccy: 'GBP' } },
    { px: { v: 12.34 } },
    { px: 12.34 },
    { px: { v: 12.34, ccy: 'GBP' }, ccy: 'USD' },
    { px: [12.34], ccy: 'GBP' },
    { px: null, ccy: 'GBP' },
  ];
  for (const [i, shape] of bad.entries()) {
    clearCache();
    const quote = { isin: 'GB0002634946', asof: `2026-09-25T06:00:0${i}Z`, ...shape };
    assert.throws(() => valuePortfolio(p, { GB0002634946: quote }), TypeError, JSON.stringify(shape));
  }
});
