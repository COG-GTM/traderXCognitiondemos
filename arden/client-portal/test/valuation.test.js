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

// ARD-4d868d: ArdenFeed schema 2.3 went live 2026-09-25 06:00Z with px as { v, ccy }.
// fixtures/ardenfeed-v2.3.json is byte-identical to the live batch that was flowing when the monitor fired.
test('values a portfolio from the live ArdenFeed v2.3 batch', () => {
  clearCache();
  const rows = valuePortfolio(positions, quotesFrom('fixtures/ardenfeed-v2.3.json'));
  assert.deepEqual(
    rows.map((r) => [r.isin, r.price, r.ccy, r.marketValue]),
    [
      ['GB0002634946', '12.4100', 'GBP', '148920.00'],
      ['CH0012032048', '270.9000', 'CHF', '216720.00'],
      ['US0378331005', '230.4000', 'USD', '345600.00'],
    ],
  );
});

test('v2.2 and v2.3 quotes carrying the same price produce identical rows', () => {
  const v23 = quotesFrom('fixtures/ardenfeed-v2.3.json');
  const v22 = {};
  for (const [isin, q] of Object.entries(v23)) v22[isin] = { isin, px: q.px.v, ccy: q.px.ccy, asof: q.asof };
  clearCache();
  const fromV22 = valuePortfolio(positions, v22);
  clearCache();
  assert.deepEqual(valuePortfolio(positions, v23), fromV22);
});

test('unknown quote shapes fail loudly instead of producing a valuation', () => {
  const base = { isin: 'GB0002634946', asof: '2026-09-25T06:00:00Z' };
  const position = [{ account: 'ACC-A', isin: 'GB0002634946', quantity: 12000 }];
  for (const shape of [
    { px: '12.41', ccy: 'GBP' },
    { px: 12.41 },
    { px: { v: 12.41 } },
    { px: { v: '12.41', ccy: 'GBP' } },
    { px: { value: 12.41, ccy: 'GBP' } },
    { px: { v: 12.41, ccy: 'gbp' } },
    { px: { v: 12.41, ccy: 'GBP' }, ccy: 'USD' },
    { px: null, ccy: 'GBP' },
    {},
  ]) {
    clearCache();
    assert.throws(() => valuePortfolio(position, { GB0002634946: { ...base, ...shape } }), /Unsupported ArdenFeed quote/, JSON.stringify(shape));
  }
});
