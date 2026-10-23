# demo_incident: prompts

Start a new Devin session on `COG-GTM/traderXCognitiondemos` with playbook
`playbook-abc2582e18fd45558d56838a14101336` (FS incident response) attached, then paste prompt 1 as is.

## Prompt 1 (the incident)

<!-- prompt-1:start -->
```
INC-2026-0925-01: client-portal valuation 5xx since 06:12 UTC, London open in 48 minutes. The alert blames deploy 31.4.2.

Repository: COG-GTM/traderXCognitiondemos, branch demo/arden-live-before. The service is arden/client-portal; its upstream feed contracts are in arden/market-data-feed-contracts. The Datadog monitor webhook, error logs, APM trace and deploy events are in arden/finserv-estate/demo/incident/fixtures/datadog/ (monitor-webhook.json, logs-search.json, apm-error-trace.json, deploy-events.json). Read AGENTS.md at the repo root first.

Investigate with the attached Datadog monitor, logs, APM trace and deploy events, find the cause, propose the smallest fix as a PR, and write the incident record. Open the PR against demo/arden-live-before (not main). Do not merge, deploy, roll back or post to Slack: draft the incident-channel thread, timeline, incident record and RCA as session attachments.
```
<!-- prompt-1:end -->

"06:12 UTC" is the time the page reached the on-call engineer, 48 minutes before the 07:00 UTC London open. The
telemetry says the first error was 06:00:41 UTC and the monitor fired at 06:04:30 UTC; the playbook tells Devin to take
times from telemetry, so Devin's timeline should correct it. That is a good moment to point at.

## Optional follow-ups while Devin works (type into the same session)

- "Before you fix anything: what is your evidence that 31.4.2 is or is not the cause?"
- "Would a rollback to 31.4.1 have fixed this? Show me the replay."
- "Which other consumers of ArdenFeed are on schema 2.2?"

## What Devin should come back with

1. Deploy 31.4.2 RULED OUT: the pre-deploy 31.4.1 code replayed on the live v2.3 batch fails with the same
   `TypeError: n.px.toFixed is not a function`; 31.4.2 served healthy 200s for about 9 hours before the feed cutover.
2. ROOT CAUSE: ArdenFeed schema 2.3 (vendor notice 11 Sep, effective 25 Sep 06:00 UTC) nests `px` as `{ v, ccy }`;
   `consumers.yaml` still lists client-portal on 2.2 (MD-1187 only covered eod-pricing-batch).
3. A PR against `demo/arden-live-before` changing `normalizeQuote` to accept 2.2 and 2.3 and throw on anything else,
   with regression, equivalence and unknown-shape tests.
4. Incident-channel thread draft, timeline, incident record (classification, client impact and closure left to named
   humans) and RCA, attached in the session.
