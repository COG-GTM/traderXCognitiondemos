// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
//   v2.2: { px: number, ccy: "GBP" }
//   v2.3 (effective 2026-09-25 06:00Z): { px: { v: number, ccy: "GBP" } }; quote-level ccy deprecated.
// Any other shape throws: a failed request is better than a wrong valuation.
const CCY = /^[A-Z]{3}$/;

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
  if (typeof q.px === 'number' && Number.isFinite(q.px) && CCY.test(q.ccy)) {
    return { px: q.px, ccy: q.ccy };
  }
  if (q.px !== null && typeof q.px === 'object' && Number.isFinite(q.px.v) && CCY.test(q.px.ccy)
      && (q.ccy === undefined || q.ccy === q.px.ccy)) {
    return { px: q.px.v, ccy: q.px.ccy };
  }
  throw new TypeError(`Unsupported ArdenFeed quote shape for ${q.isin}: ${JSON.stringify(q)}`);
}
