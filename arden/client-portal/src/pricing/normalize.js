// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
//   v2.2: { px: number, ccy: "GBP" }
//   v2.3 (effective 2026-09-25 06:00Z): { px: { v: number, ccy: "GBP" } }; quote-level ccy deprecated.
// Any other shape throws: a failed request is better than a wrong valuation.
const isCcy = (c) => typeof c === 'string' && /^[A-Z]{3}$/.test(c);

export function normalizeQuote(q) {
  const { px, ccy } = priceOf(q);
  return {
    isin: q.isin,
    px,
    ccy,
    asof: q.asof,
  };
}

function priceOf(q) {
  if (typeof q.px === 'number' && Number.isFinite(q.px) && isCcy(q.ccy)) {
    return { px: q.px, ccy: q.ccy };
  }
  // v2.3: px.ccy is the price currency; the deprecated quote-level ccy is ignored.
  if (q.px !== null && typeof q.px === 'object' && Number.isFinite(q.px.v) && isCcy(q.px.ccy)) {
    return { px: q.px.v, ccy: q.px.ccy };
  }
  throw new TypeError(`Unsupported ArdenFeed quote shape for ${q.isin}: ${JSON.stringify(q)}`);
}
