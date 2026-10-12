import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeQuote } from '../src/pricing/normalize.js';

test('normalises an ArdenFeed v2.2 quote (bare px, quote-level ccy)', () => {
  assert.deepEqual(
    normalizeQuote({ isin: 'GB0002634946', px: 12.34, ccy: 'GBP', asof: '2026-09-24T16:35:00Z' }),
    { isin: 'GB0002634946', px: 12.34, ccy: 'GBP', asof: '2026-09-24T16:35:00Z' },
  );
});

test('normalises an ArdenFeed v2.3 quote (px wrapped as { v, ccy })', () => {
  assert.deepEqual(
    normalizeQuote({ isin: 'GB0002634946', px: { v: 12.41, ccy: 'GBP' }, asof: '2026-09-25T06:00:00Z' }),
    { isin: 'GB0002634946', px: 12.41, ccy: 'GBP', asof: '2026-09-25T06:00:00Z' },
  );
});

test('rejects an unrecognised px shape with a descriptive error', () => {
  assert.throws(() => normalizeQuote({ isin: 'X', px: { value: 1 }, asof: 't' }), /unsupported px/);
});

test('rejects null, empty or non-numeric px instead of coercing to zero', () => {
  for (const px of [null, '', '12.4', true, { v: null, ccy: 'GBP' }, { v: '', ccy: 'GBP' }]) {
    assert.throws(() => normalizeQuote({ isin: 'X', px, ccy: 'GBP', asof: 't' }), /unsupported px/);
  }
});

test('rejects a quote with no currency', () => {
  assert.throws(() => normalizeQuote({ isin: 'X', px: { v: 12.41 }, asof: 't' }), /missing ccy/);
  assert.throws(() => normalizeQuote({ isin: 'X', px: 12.41, asof: 't' }), /missing ccy/);
});
