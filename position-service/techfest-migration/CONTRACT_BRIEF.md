# Contract brief: positions v1 -> v2 (Synthetic demonstration)

This brief is the intent-and-constraints artifact for migrating consumers of the positions API in the
FINOS TraderX reference application. It is synthetic demonstration material; the data is fabricated.

## Endpoints

| | Legacy **v1** (deprecated) | Approved **v2** |
|---|---|---|
| Request | `GET {position_service_url}/positions/{accountId}` | `GET {position_service_url}/v2/positions?accountId=&cursor=&limit=` |
| Response | one JSON **array** of positions | `{ "items": [ ...positions ], "nextCursor": string \| null }` |
| Paging | none (whole account in one response) | cursor pagination, `limit` default **10**, max 100 |
| Order | unspecified | deterministic: `security` ascending |

Both endpoints are served by `position-service` from the **same `POSITIONS` rows**, so a v1-backed and a
v2-backed report for the same account must agree row for row.

### Walking v2 pages

1. Call with `accountId` and no `cursor`.
2. Append `items`. If `nextCursor` is a string, call again with `cursor=<nextCursor>` (URL-encode it).
3. Stop only when `nextCursor` is JSON `null`. The last page can be full-size; **`null` is the only
   terminal signal** — a short page or an empty `items` array is not.
4. The cursor is opaque. Do not parse, construct or compare it.
5. Invalid `cursor` or `limit` returns HTTP 400.

## Field mapping

| v1 field | v1 type | v2 field | v2 type | Meaning / rule |
|---|---|---|---|---|
| `accountId` | integer | `accountId` | integer | Account identifier. Unchanged. |
| `security` | string | `security` | string | Position identifier within the account (ticker). Unchanged; unique per account. |
| `quantity` | integer | `quantity` | integer | Units held; may be negative (short). Unchanged. |
| `updated` | legacy timestamp string, e.g. `2026-09-22T16:00:00.000+00:00` | `asOf` | ISO-8601 UTC string, e.g. `2026-09-22T16:00:00Z` | Same instant. Compare as instants, not as strings. |
| `currency` | 3-letter code | `currency` | 3-letter code | ISO-4217 valuation currency of the position. **Always present**, including when `marketValue` is `null`; use it for per-currency grouping. |
| — | — | `marketValue.currency` | 3-letter code | Always equals the item's `currency` when `marketValue` is not null. |
| `marketValue` | JSON **number** or `null` | `marketValue` | `{ "amount": string, "currency": string }` or `null` | See units and null behaviour below. |

`asOf` is the time the position was last valued/updated; it is **not** the response time.

## Units and precision

* v2 `marketValue.amount` is a **decimal string** already rendered at the currency's minor-unit scale:
  `JPY` and `KRW` 0 decimals (`"27400000"`), all other fixture currencies 2 decimals (`"1234567.89"`).
* Consumers must keep amounts as strings / integer minor units (BigInt, BigDecimal). **Never** parse an
  amount into an IEEE float for arithmetic or comparison.
* v1 `marketValue` is a JSON number carrying up to 4 decimals. When a v1 row is shown for comparison it is
  rendered to the currency scale immediately; no float leaves the adapter.

## Null behaviour (unknown stays unknown)

* An unknown market value is `marketValue: null` in both APIs. It is never `0`, never `""`, never omitted.
  The position still has a `currency` and still counts toward that currency's `count`.
* The UI renders an unknown value as the literal word **`unknown`**.
* Fixtures guarantee at least one unknown value on a page **after** page 1, so a consumer that stops
  paging early will not merely lose rows; it will also lose the `incomplete` flag.

## Aggregation rules

1. Group by `currency`. Report **count** and **total market value per currency**.
2. Never sum amounts of unlike currencies. Never convert currencies, silently or otherwise.
3. A per-currency total includes only known values, and the summary is labeled **`incomplete`** whenever
   at least one position in that currency has an unknown market value. Summaries with no unknowns are
   `complete`.
4. Totals are computed with integer minor-unit arithmetic and rendered at the currency scale.
5. Row order in the report is `security` ascending (same as the v2 contract).

## Compatibility requirements

* v1 remains served for the demo. After migration, **the only permitted v1 call in the React front-end is
  the Portfolio report's "Legacy v1 (comparison)" toggle** (`web-front-end/react/src/report/legacyPositions.ts`).
  The primary execution path — the shared adapter `hooks/GetPositions.ts`, the blotter, and the report's
  "Current" source — must not call `/positions/{accountId}`.
* The v2 contract and `position-service` are frozen for the migration; consumer changes only.
* Behaviour must be identical for any `limit` in `1..100`; the consumer must not depend on the default.
* Approved expectations for the fixture account **77007** are in
  `fixtures/report-expectations.json` (26 positions, 3 pages at the default limit, currencies CHF/EUR/GBP/JPY/USD,
  unknown values on `MC` (EUR, page 2) and `T9984` (JPY, page 3)). Recorded v2 pages are in
  `fixtures/v2-pages-77007.json`; the v1 array in `fixtures/v1-positions-77007.json`.

## Where the consumer lives

* Adapter: `web-front-end/react/src/hooks/GetPositions.ts` (returns positions for one account).
* Consumers: `web-front-end/react/src/Datatable/Datatable.tsx` (blotter, AG Grid) and
  `web-front-end/react/src/report/` (Portfolio report; `useReportPositions.ts` feeds the "Current" source
  through the adapter, `legacyAdapter.ts` maps v1 rows onto the report model `types.ts`).
* Tests the migrated consumer must keep green: `web-front-end/react/src/report/*.test.ts*` and the
  position-service contract/pagination tests under `position-service/src/test/java/.../techfest/`.
