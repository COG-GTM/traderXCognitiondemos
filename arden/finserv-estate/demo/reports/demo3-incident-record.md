# Incident record — INC-2026-0925-01 (DRAFT, for ops-risk review)

| Field | Value | Source |
|---|---|---|
| Service | client-portal (institutional client portal, intraday valuations) | monitor scope `service:client-portal,env:prod` |
| Detected | 2026-09-25T06:04:30Z by Datadog monitor 183220417 (`valuation 5xx rate > 5%`) | Datadog webhook |
| Impact start | 2026-09-25T06:00:41Z (first 500 on `/api/portfolio/valuation`) | Datadog logs |
| Last known good | 2026-09-25T05:58:33Z | Datadog logs |
| Impact | 100% of intraday valuation requests failing; positions/settlement routes unaffected | monitor body, logs |
| Root cause | ROOT CAUSE — ArdenFeed v2.3 went live 06:00 UTC with px as an object; client-portal's normaliser (schema 2.2) passes the object through and toFixed fails | investigation step 3 |
| Contributing | vendor change notice (11 Sep) actioned for eod-pricing-batch only; consumer register listed client-portal on 2.2; no parallel run offered | market-data-feed-contracts |
| False lead | Lead A — latest deploy 31.4.2 (PORTAL-3312) broke it — ruled out by replaying the live batch on the pre-deploy release | investigation step 2 |
| Fix | branch `incident/inc-2026-0925-01-ardenfeed-v23` — normaliser accepts 2.2/2.3, rejects unknown shapes; 4/4 tests, lint ok | investigation step 4 |
| Deployed | **not yet** — @ardencm/client-digital + release-bot | — |
| Data integrity | no valuations were served wrong; requests failed closed (500) | logs: no 200 with schema 2.3 before the fix |
| Client impact | to be confirmed by client-digital (portal sessions 06:00–resolution) | — |
| Regulatory classification | **not assessed here.** DORA Art. 18 major-incident criteria (clients affected, duration, geographic spread, data losses, criticality, economic impact) need ops-risk input; if major, initial notification is due 4h after classification / 24h after awareness | human |
| Owners | service: @ardencm/client-digital · feed: @ardencm/market-data · vendor: vendor management | CODEOWNERS |

Generated 2026-10-01T14:28:41Z from `demo/out/incident/investigation.json`. This record is a draft produced by the investigation; the classification, client-impact and closure fields are owned by humans.
