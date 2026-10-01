// Normalises an ArdenFeed quote (schema v2.2) into { isin, px, ccy, asof }.
export function normalizeQuote(q) {
  return {
    isin: q.isin,
    px: q.px,
    ccy: q.ccy,
    asof: q.asof,
  };
}
