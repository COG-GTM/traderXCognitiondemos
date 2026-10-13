// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
// v2.2: px is a number, ccy is on the quote. v2.3 (effective 2026-09-25 06:00Z): px is { v, ccy }, quote-level ccy deprecated.
// Anything else throws: a failed request is better than a wrong valuation.
export function normalizeQuote(q) {
  if (q == null || typeof q !== 'object') {
    throw new TypeError(`ArdenFeed quote missing or not an object: ${JSON.stringify(q)}`);
  }
  const { isin, px, asof } = q;
  if (typeof px === 'number' && Number.isFinite(px) && typeof q.ccy === 'string') {
    return { isin, px, ccy: q.ccy, asof };
  }
  if (px && typeof px === 'object' && typeof px.v === 'number' && Number.isFinite(px.v) && typeof px.ccy === 'string') {
    if (typeof q.ccy === 'string' && q.ccy !== px.ccy) {
      throw new TypeError(`ArdenFeed quote ${isin}: px.ccy ${px.ccy} conflicts with quote ccy ${q.ccy}`);
    }
    return { isin, px: px.v, ccy: px.ccy, asof };
  }
  throw new TypeError(`Unsupported ArdenFeed quote shape for ${isin}: px=${JSON.stringify(px)} ccy=${JSON.stringify(q.ccy)}`);
}
