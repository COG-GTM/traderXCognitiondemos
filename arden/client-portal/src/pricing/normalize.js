// Normalises an ArdenFeed quote into { isin, px, ccy, asof }.
// Schema 2.2 sends `px` as a bare number with a quote-level `ccy`;
// schema 2.3 (effective 2026-09-25) wraps it as `px: { v, ccy }` and drops the quote-level `ccy`.
export function normalizeQuote(q) {
  if (!q) throw new Error('ArdenFeed quote missing');
  const wrapped = q.px !== null && typeof q.px === 'object';
  const px = wrapped ? q.px.v : q.px;
  const ccy = wrapped ? q.px.ccy ?? q.ccy : q.ccy;
  if (typeof px !== 'number' || !Number.isFinite(px)) {
    throw new Error(`ArdenFeed quote ${q.isin}: unsupported px ${JSON.stringify(q.px)}`);
  }
  if (typeof ccy !== 'string' || ccy === '') {
    throw new Error(`ArdenFeed quote ${q.isin}: missing ccy`);
  }
  return { isin: q.isin, px, ccy, asof: q.asof };
}
