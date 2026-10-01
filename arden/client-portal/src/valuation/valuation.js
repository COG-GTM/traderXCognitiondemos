import { normalizeQuote } from '../pricing/normalize.js';

const rowCache = new Map();

// Memoised per (isin, asof) — added in PORTAL-3312 (deploy 2026-09-24 21:05Z) to speed up the positions table.
export function valuationRow(position, quote) {
  const key = `${position.isin}|${quote.asof}`;
  if (rowCache.has(key)) return rowCache.get(key);
  const n = normalizeQuote(quote);
  const row = {
    isin: n.isin,
    quantity: position.quantity,
    price: n.px.toFixed(4),
    ccy: n.ccy,
    marketValue: (position.quantity * n.px).toFixed(2),
    asof: n.asof,
  };
  rowCache.set(key, row);
  return row;
}

export function valuePortfolio(positions, quotesByIsin) {
  return positions.map((p) => valuationRow(p, quotesByIsin[p.isin]));
}

export function clearCache() {
  rowCache.clear();
}
