# RCA — INC-2026-0925-01: client-portal valuation failures after ArdenFeed v2.3 cutover (DRAFT)

## Summary
At 06:00 UTC on 25 Sep 2026 ArdenFeed switched its quote schema from 2.2 to 2.3 (`px` number → `{ v, ccy }` object) with no parallel run.
client-portal's quote normaliser still assumed 2.2, so every intraday valuation request failed with `TypeError: n.px.toFixed is not a function`.
Datadog paged at 2026-09-25T06:04:30Z. The most recent deploy (31.4.2, memoisation of the same function, 8.9h earlier) looked responsible and was not.

## What happened
- alert: [Triggered on {service:client-portal,env:prod}] client-portal / valuation 5xx rate fired 2026-09-25T06:04:30Z
- first error log 2026-09-25T06:00:41Z v31.4.2: n.px.toFixed is not a function
- stack → src/valuation/valuation.js:13: `price: n.px.toFixed(4),`
- replay market-data-feed-contracts/samples/latest-batch.json through valuePortfolio on main (31.4.2): FAIL TypeError: n.px.toFixed is not a function

## The lead that was wrong, and how it was ruled out
- deploy 31.4.2 at 2026-09-24T21:05:12Z by release-bot (PORTAL-3312 feature: memoise valuation rows for positions table perf — commit 9c41f0e — release-bot) — 8.9h before the first error
- git log -L on the failing line: 40c204d Arden Platform Bot 2026-09-10 feature: PORTAL-3270 settlement status widget — the line predates the deploy, but 31.4.2 rewrote this function (memoisation), so the lead stays plausible until tested
- checkout pre-deploy commit e0fe2a5 (31.4.1, the release before v31.4.2) and replay the live batch: FAIL TypeError: n.px.toFixed is not a function
- same pre-deploy code with yesterday's batch (schema 2.2): OK [["GB0002634946","12.3400","GBP","148080.00"],["CH0012032048","271.5000","CHF","217200.00"],["US0378331005","231.1200","USD","346680.00"]]
- 7 healthy 200s on 31.4.2 between 2026-09-24T21:06:11Z and 2026-09-25T05:58:33Z

**RULED OUT — pre-deploy code fails identically on today's batch and works on yesterday's; a rollback would not have fixed it.** A rollback — the standard first move — would have kept the portal down.

## Root cause
- last healthy request 2026-09-25T05:58:33Z carried feed schema 2.2; first error 2026-09-25T06:00:41Z carries schema 2.3
- batch diff for GB0002634946: px 12.34 → {"v": 12.41, "ccy": "GBP"}; quote-level ccy present → absent
- market-data-feed-contracts/ardenfeed/VENDOR-NOTICE-2026-09-11.md: effective 25 Sep 2026 06:00 UTC (no parallel run)
- market-data-feed-contracts/consumers.yaml: `- repo: client-portal` `schema: "2.2"   # intraday valuation; not updated for 2.3 (MD-1187 scope was EOD only)` — client-portal was never moved to 2.3
- market-data-feed-contracts history: b7c6869 2026-09-25T06:02:14+00:00 feed-ingest-bot — feature: auto-ingest ArdenFeed schema v2.3 + first live batch sample | d210ca9 2026-09-22T11:05:00+00:00 Arden Platform Bot — feature: MD-1187 eod-pricing-batch moves to ArdenFeed 2.3 | 65a9eeb 2026-09-11T14:22:00+00:00 Tom Okafor — feature: file ArdenFeed v2.3 change notice (effective 25 Sep) | 85bd0e2 2026-06-02T10:00:00+00:00 Arden Platform Bot — feature: ArdenFeed v2.2 schema and consumer register
- notice says: 'Internal tracking: MD-1187 (eod-pricing-batch updated 22 Sep). No ticket was raised for client-portal.'

**ROOT CAUSE — ArdenFeed v2.3 went live 06:00 UTC with px as an object; client-portal's normaliser (schema 2.2) passes the object through and toFixed fails.**

## Fix
- tests before the fix (main): tests 1 pass 1 fail 0
- tests after: tests 4 pass 4 fail 0 (3 new: v2.3 batch, 2.2≡2.3, unknown shape throws)
- npm run lint: ok
- replay live batch (2.3): OK [["GB0002634946","12.4100","GBP","148920.00"],["CH0012032048","270.9000","CHF","216720.00"],["US0378331005","230.4000","USD","345600.00"]]
- replay yesterday's batch (2.2): OK [["GB0002634946","12.3400","GBP","148080.00"],["CH0012032048","271.5000","CHF","217200.00"],["US0378331005","231.1200","USD","346680.00"]]
- branch incident/inc-2026-0925-01-ardenfeed-v23, reviewers @ardencm/client-digital, @ardencm/market-data

Rollout is a human decision. The fix fails loudly on any *third* shape rather than silently passing objects through.

## Why this was not caught earlier
- Vendor notice reached market-data (11 Sep) and was actioned for eod-pricing-batch only; consumers.yaml listed client-portal as a 2.2 consumer and nobody was told.
- No consumer contract test in market-data-feed-contracts exercises client-portal's parser against the new schema.
- No monitor on quote schema version / parse failures; detection came from the 5xx rate 4 minutes after cutover.
- Vendor gave no parallel run; whether to require one contractually is a vendor-management decision.

## Recommendations (owners decide; nothing below is done)
1. market-data: route vendor notices to every consumer in `consumers.yaml`, not to the ticket's scope. Owner @ardencm/market-data.
2. market-data-feed-contracts: add a consumer contract test per registered consumer that runs the consumer's parser against each schema sample. Owner @ardencm/market-data + consumers.
3. client-portal: monitor on parse failures / quote schema version, alerting before the 5xx rate does. Owner @ardencm/client-digital.
4. Vendor management: require a parallel-run window in the ArdenFeed contract. Owner vendor management.

## What this RCA does not decide
Regulatory classification, client communications and whether 31.4.2's memoisation should stay are outside the investigation's remit.

Generated 2026-10-01T14:28:41Z.
