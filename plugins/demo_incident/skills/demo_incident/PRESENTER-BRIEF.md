# demo_incident: presenter brief

**Goal.** At 06:12 UTC, 48 minutes before the London open, Arden's client portal can't value portfolios, and the Datadog alert blames last night's deploy 31.4.2. Live on stage, Devin proves the deploy is innocent, finds the real cause (vendor feed ArdenFeed 2.3 changed the price field), opens the smallest fix PR and writes the incident record. Nothing gets merged, deployed or posted to Slack.

**Time:** about 12 minutes on stage, about 9.5 minutes of that is Devin (rehearsal: fix PR at 3:19, root cause at 5:06, finished at 9:34).

## Tabs to open before you start

| # | Tab | Why |
|---|---|---|
| 1 | [BEFORE portal: valuation](http://localhost:3200/api/portfolio/valuation) (on the setup VM; use the setup session's "BEFORE portal" preview link) | The outage: 500 `valuation_failed` |
| 2 | [Datadog alert (monitor-webhook.json)](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/finserv-estate/demo/incident/fixtures/datadog/monitor-webhook.json) | The alert that blames deploy 31.4.2 |
| 3 | **Live Devin session**: the setup session starts it for you as a child (FS incident response playbook, prompt already sent) and posts the link | Beats 3-9 happen here |
| 4 | [Fallback fix PR #122](https://github.com/COG-GTM/traderXCognitiondemos/pull/122) | Keep it hidden; only for when the live run stalls |

## Prompt (already sent in tab 3; use this only to restart from [Devin home](https://devin-gtm.devinenterprise.com/))

Starting it with `!fs_incident` attaches the FS incident response playbook for you. If the macro isn't offered, attach [FS incident response](https://devin-gtm.devinenterprise.com/settings/playbooks/abc2582e18fd45558d56838a14101336) by hand and delete the first line.

```
!fs_incident
INC-2026-0925-01: client-portal valuation 5xx since 06:12 UTC, London open in 48 minutes. The alert blames deploy 31.4.2.

Repository: COG-GTM/traderXCognitiondemos, branch demo/arden-live-before. The service is arden/client-portal; its upstream feed contracts are in arden/market-data-feed-contracts. The Datadog monitor webhook, error logs, APM trace and deploy events are in arden/finserv-estate/demo/incident/fixtures/datadog/ (monitor-webhook.json, logs-search.json, apm-error-trace.json, deploy-events.json). Read AGENTS.md at the repo root first.

Investigate with the attached Datadog monitor, logs, APM trace and deploy events, find the cause, propose the smallest fix as a PR, and write the incident record. Open the PR against demo/arden-live-before (not main). Do not merge, deploy, roll back or post to Slack: draft the incident-channel thread, timeline, incident record and RCA as session attachments.
```

## Beats

| Time | Beat | Show | Say |
|---|---|---|---|
| 0:00 | **1. The outage** | Tab 1: 500 `valuation_failed` | "Clients can see their settlements but not what their portfolio is worth, 48 minutes before the open." |
| 1:00 | **2. The alert** | Tab 2, the `body` line: "Last deploy: client-portal 31.4.2" | "The alert points at last night's deploy, so everyone wants to roll back. Let's see if Devin agrees." |
| 1:30 | **3. Hand it to Devin** | Tab 3: scroll to the top and show the prompt | "I haven't told Devin the cause. It gets the same alert, logs and trace the on-call engineer got." |
| 2:00 | **4. Reproduce** | The Devin session's shell: `TypeError: n.px.toFixed is not a function` at [valuation.js line 13](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/client-portal/src/valuation/valuation.js#L13) | "Same error as production, reproduced locally before anyone touches code." |
| 3:00 | **5. Is it the deploy?** | The Devin session: replay on 31.4.1 fails the same way | "The code from before the deploy breaks too, so a rollback would have used up 20 minutes and fixed nothing." Optional follow-up: "Would a rollback to 31.4.1 have fixed this? Show me the replay." |
| 5:00 | **6. The real cause** | [Vendor notice line 6](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/market-data-feed-contracts/ardenfeed/VENDOR-NOTICE-2026-09-11.md#L6) and [consumers.yaml line 8](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/market-data-feed-contracts/consumers.yaml#L8) | "The vendor changed the price field at 06:00. The portal was never moved to the new format; the deploy just happened to be there." |
| 6:00 | **7. Smallest fix** | **The PR link Devin posts in the session** (new PR against `demo/arden-live-before`): one function, [`normalizeQuote`](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/client-portal/src/pricing/normalize.js#L2), plus tests | "One function now reads both feed versions and rejects anything else. Devin opened the PR; your engineers review and merge it." |
| 8:00 | **8. After** | The Devin session's replay: 200 with GBP 148,920.00, CHF 216,720.00, USD 345,600.00 | "Same live data, now valued correctly." |
| 9:00 | **9. Paperwork** | The Devin session's attachments: Slack thread draft, timeline, incident record, RCA | "Timeline from telemetry: the first error was 06:00:41, not 06:12. Classification and client impact are left for named humans to decide. Nothing went to Slack." |
| 11:00 | **Close** | | "Evidence before action, the deploy ruled out by replay, a one-function fix with tests, and the record written while the engineers decide." |

## Which PR to use

| Situation | PR |
|---|---|
| Live run works (normal case) | The new PR Devin opens in the tab 3 session (beat 7). |
| Live run stalls or is past 8:00 | [PR #122](https://github.com/COG-GTM/traderXCognitiondemos/pull/122): same fix, ruled-out evidence and tests, plus a before/after video in the comments. Say "this is the fix Devin produced on an earlier run of the same incident." |
| Someone asks for screenshots or "show me it was ruled out" | [PR #118](https://github.com/COG-GTM/traderXCognitiondemos/pull/118): "What was ruled out" section and before/after browser screenshots. |

Both fallback PRs are open, mergeable and green. Do not merge them.

## Checklist: what Devin should come back with

- [ ] Deploy 31.4.2 **ruled out** by replay on 31.4.1 (same TypeError), not just by looking at timing
- [ ] Root cause: ArdenFeed 2.3 nests `px` as `{ v, ccy }`; `consumers.yaml` still has client-portal on 2.2
- [ ] One PR against `demo/arden-live-before` that changes `normalizeQuote`, with regression, equivalence and unknown-shape tests
- [ ] Valuation goes from 500 to 200 with GBP, CHF and USD rows
- [ ] Slack thread draft, timeline, incident record and RCA attached; nothing merged, deployed, rolled back or posted

## If it goes wrong

- **Devin accepts the deploy as the cause:** type "Before you fix anything: what is your evidence that 31.4.2 is or is not the cause?"
- **Still working at 8:00:** talk through the finished beats, then open PR #122.
- **Reviewer asks about a quote whose old `ccy` disagrees with `px.ccy`:** the fix rejects it (returns 500) rather than guessing a currency. "A wrong price is worse than an error; your team decides."
