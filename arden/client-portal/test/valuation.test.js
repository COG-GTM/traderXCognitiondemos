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

test('values a portfolio from an ArdenFeed v2.3 batch', () => {
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
