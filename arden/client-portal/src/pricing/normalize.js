// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
//   v2.2: px is a number, ccy is on the quote.
//   v2.3 (effective 2026-09-25 06:00Z): px is { v: number, ccy: string }; quote-level ccy is deprecated.
// Any other shape throws: a failed valuation is recoverable, a wrong price or currency on a client statement is not.
const CCY = /^[A-Z]{3}$/;
const isPrice = (v) => typeof v === 'number' && Number.isFinite(v);
const isCcy = (c) => typeof c === 'string' && CCY.test(c);

export function normalizeQuote(q) {
  const px = q?.px;
  if (isPrice(px) && isCcy(q.ccy)) {
    return { isin: q.isin, px, ccy: q.ccy, asof: q.asof };
  }
  if (px !== null && typeof px === 'object' && isPrice(px.v) && isCcy(px.ccy) && (q.ccy === undefined || q.ccy === px.ccy)) {
    return { isin: q.isin, px: px.v, ccy: px.ccy, asof: q.asof };
  }
  throw new Error(`unsupported ArdenFeed quote shape for ${q?.isin}: px=${JSON.stringify(px)} ccy=${JSON.stringify(q?.ccy)}`);
}
