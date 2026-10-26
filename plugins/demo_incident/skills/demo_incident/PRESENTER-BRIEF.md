# demo_incident: presenter brief

**Goal:** At 06:12 UTC, 48 minutes before the London open, Arden's portfolio valuation is down and the alert blames last night's deploy. Devin proves the deploy is innocent, finds the vendor feed change, opens a one-function fix PR and writes the incident record. About 12 minutes; nothing gets merged, deployed or posted.

## Tabs
1. [BEFORE portal]({{BEFORE_URL}}) (500 `valuation_failed`)
2. [Datadog alert](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/finserv-estate/demo/incident/fixtures/datadog/monitor-webhook.json)
3. [Live Devin session]({{SESSION_URL}}) (prompt already sent)
4. Hidden: [AFTER portal]({{AFTER_URL}}) and [PR #122](https://github.com/COG-GTM/traderXCognitiondemos/pull/122) (fallback fix with the before/after video)

## Beats
| Time | Beat | Show | PR |
|---|---|---|---|
| 0:00 | Clients can't see what their portfolio is worth. | Tab 1 | |
| 1:00 | The alert blames deploy 31.4.2, so everyone wants to roll back. | Tab 2, "Last deploy" line | |
| 1:30 | Devin gets the same alert and logs as on-call. | Tab 3, the prompt | |
| 2:00 | Same error as production, reproduced first. | Tab 3, `TypeError` ([line](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/client-portal/src/valuation/valuation.js#L13)) | |
| 3:00 | The pre-deploy code breaks too, so a rollback would fix nothing. | Tab 3, 31.4.1 replay | |
| 5:00 | The vendor changed the price field; the portal was never updated. | [Notice](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/market-data-feed-contracts/ardenfeed/VENDOR-NOTICE-2026-09-11.md#L6), [consumers.yaml](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/market-data-feed-contracts/consumers.yaml#L8) | |
| 6:00 | One-function fix with tests; your engineers review it. | Tab 3, Devin's PR | **{{LIVE_PR}}** |
| 8:00 | Same data, valued: GBP 148,920 / CHF 216,720 / USD 345,600. | Tab 3 replay | |
| 9:00 | Timeline, incident record, RCA and Slack draft; humans classify. | Tab 3 attachments | |

## Which PR
- Normal: {{LIVE_PR}}, opened live in tab 3.
- Stalled or past 8:00: [#122](https://github.com/COG-GTM/traderXCognitiondemos/pull/122), "the fix Devin produced on an earlier run."
- "Show me it was ruled out": [#118](https://github.com/COG-GTM/traderXCognitiondemos/pull/118).

Never merge them.

## Check
- [ ] 31.4.2 ruled out by replay, not by timing
- [ ] Cause: ArdenFeed 2.3 nests `px` as `{ v, ccy }`
- [ ] One PR against `demo/arden-live-before` with tests
- [ ] Record, timeline, RCA attached; nothing posted

**If Devin blames the deploy:** "What is your evidence that 31.4.2 is or is not the cause?"
