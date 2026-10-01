// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
// Schema 2.2: { px: number, ccy }            Schema 2.3+: { px: { v: number, ccy } } (quote-level ccy deprecated)
export function normalizeQuote(q) {
  const px = typeof q.px === 'object' && q.px !== null ? q.px.v : q.px;
  const ccy = (typeof q.px === 'object' && q.px !== null ? q.px.ccy : undefined) ?? q.ccy;
  if (typeof px !== 'number' || typeof ccy !== 'string') {
    throw new TypeError(`unrecognised ArdenFeed quote shape for ${q.isin}: ${JSON.stringify(q.px)}`);
  }
  return { isin: q.isin, px, ccy, asof: q.asof };
}
