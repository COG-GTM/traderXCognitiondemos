// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
// v2.2: px is a number, ccy is quote-level. v2.3 (effective 2026-09-25 06:00Z): px is { v, ccy }; quote-level ccy is
// deprecated. Any other shape throws: a failed valuation is better than a wrong number.
const CCY = /^[A-Z]{3}$/;

export function normalizeQuote(q) {
  if (!q || typeof q !== 'object') throw new TypeError('ArdenFeed quote missing');
  const { px } = q;
  let value;
  let ccy;
  if (typeof px === 'number') {
    value = px;
    ccy = q.ccy;
  } else if (px && typeof px === 'object' && !Array.isArray(px)) {
    value = px.v;
    ccy = px.ccy;
    if (q.ccy !== undefined && q.ccy !== ccy) {
      throw new TypeError(`ArdenFeed quote ${q.isin}: px.ccy ${ccy} disagrees with quote ccy ${q.ccy}`);
    }
  } else {
    throw new TypeError(`ArdenFeed quote ${q.isin}: unsupported px shape ${JSON.stringify(px)}`);
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new TypeError(`ArdenFeed quote ${q.isin}: px value is not a finite number`);
  }
  if (typeof ccy !== 'string' || !CCY.test(ccy)) {
    throw new TypeError(`ArdenFeed quote ${q.isin}: missing or invalid ccy`);
  }
  return {
    isin: q.isin,
    px: value,
    ccy,
    asof: q.asof,
  };
}
