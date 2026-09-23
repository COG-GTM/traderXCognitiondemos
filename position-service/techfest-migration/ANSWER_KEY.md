# ANSWER KEY — presenter only (Synthetic demonstration)

Not referenced by `TASK_PROMPT.md`. Do not share with the remediation session.

## What a correct migration looks like

1. `hooks/GetPositions.ts` calls `${Environment.position_service_url}/v2/positions?accountId=..&limit=..`,
   loops while `nextCursor !== null` passing `cursor=encodeURIComponent(nextCursor)`, concatenates `items`,
   and returns positions. The blotter needs `security`, `quantity` and a date: map `asOf` (ISO string) where
   it previously used `updated`. The report needs the `ReportPosition` model (`src/report/types.ts`):
   v2 items are `{accountId, security, quantity, asOf, currency, marketValue}` and map 1:1 onto it
   (`currency` is top-level so unknown-value rows keep their currency and still count in the summary).
   The trap is subtler: `marketValue` may be `null`, so `marketValue.currency` must never be used for
   grouping; and the loop must continue while `nextCursor !== null` (page 3 is short, page 2 is full).
2. `useReportPositions.ts` stops mapping through `fromLegacyPosition`; it consumes the adapter's output.
3. `legacyPositions.ts` is left alone (permitted comparison toggle).
4. Tests: `src/report/PortfolioReport.test.tsx` mocks `fetch` against v1 today. The migrated consumer must
   update the mock to serve the pages from `fixtures/v2-pages-77007.json` (matching on `cursor`), and the
   fixture assertions must still pass. `summarize.test.ts` already encodes the v2-walk expectation.
5. Verify in the browser: Portfolio report, account 77007, "Current" shows 26 positions, EUR and JPY
   `incomplete`, `MC` and `T9984` `unknown`; the "Legacy v1 (comparison)" toggle shows identical numbers.

## Common wrong answers to watch for

| Symptom | Cause |
|---|---|
| Report shows 10 or 20 positions | only the first page (or first two) fetched; `nextCursor` ignored or loop stops on a full page |
| EUR total `567180.00` but not labeled `incomplete`, or `MC` missing | `MC` (page 2) dropped or null treated as 0 |
| JPY total `35860000.00` | currency scale ignored (JPY is 0 decimals) |
| USD total off by cents / `1e+…` | float arithmetic on amounts |
| One "Total" across currencies | cross-currency sum (forbidden) |
| Blotter date column blank | `updated` no longer present; `asOf` not mapped |
| Comparison toggle also switched to v2 | `legacyPositions.ts` rewritten (allowed exception removed) |

## Seeded defect scenario (presenter only, never attributed to Devin)

`fixtures/v2-pages-77007-seeded-defect.json` is a recorded page set whose page 2 silently drops one row.
Run it with the labeled command in README ("Seeded-defect scenario"); it is expected to be **red** and is
NOT part of the default suite. Use it only to show what the fixture expectations catch.

## Approved numbers (account 77007)

| Currency | Count | Total | Status |
|---|---|---|---|
| CHF | 5 | 421,294.00 | complete |
| EUR | 8 | 567,180.00 | incomplete (`MC` unknown, page 2) |
| GBP | 7 | 405,580.00 | complete |
| JPY | 3 | 35,860,000 | incomplete (`T9984` unknown, page 3) |
| USD | 3 | -49,500.00 | complete |

26 positions, 3 pages at limit 10 (10 / 10 / 6). Pages are recorded in `fixtures/v2-pages-77007.json`.
