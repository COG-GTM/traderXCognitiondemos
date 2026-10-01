# Production incident — from monitor alert to root cause, fix branch and incident record

## Overview
A Datadog monitor has fired on a customer-facing service. Investigate it the way a good on-call engineer would: reproduce
first, then treat every candidate cause as a hypothesis with a test — including the obvious one (the latest deploy) — and
only call something the root cause once the alternative has been ruled out with evidence. Fix on a branch with a regression
test; never deploy, roll back or change vendor contracts. Leave behind a Slack thread, a timeline, an incident record and an
RCA whose classification fields are explicitly left to humans. Only ever create the todo list for the current phase.

## What's Needed From User
- The alert: Datadog webhook body or monitor link (title, query, scope, fired time), and the Slack incident channel
- Repositories for the service and for any upstream data contracts it consumes (feeds, schemas, vendor notices)
- Where deploy history lives (Datadog deploy events, `deploys/history.json`, release tags)
- Who owns rollout decisions and regulatory classification (CODEOWNERS team, ops risk)

## Reference (all phases)
- Estate helpers: `demo/incident/ingest.py` (fixture or `--source datadog`), `demo/incident/investigate.py`, `demo/incident/report.py`, `demo/run-demo3.sh`
- Each commit message must contain `feature` or `bug`. Every PR description ends with the line `Devin-Org: engineering`.
- Correlation is not causation: a deploy that precedes the error is a lead, not a finding, until the pre-deploy build is replayed against the same input.
- Requests that fail closed (5xx) are a smaller incident than requests that return wrong numbers; say which happened.

<phase name="Ingest and reproduce" id="1">
## Ingest and reproduce

1. Normalise the alert plus its context into one input: monitor title/query/scope/fired time, error logs (first error, stack,
   version, any feed/schema attributes), the last healthy requests, APM trace, and deploy events for the service in the last 24h.
2. Read the stack: which file and line, which version. Find the exact line in the repository at that version.
3. Reproduce locally with the *live* input (the batch/message/request that was flowing when the error started), not a fixture.
   The reproduction must produce the same exception type and message.
4. Record: first error time, last healthy time, and every deploy between them.

<verification>
- The reproduction fails with the same exception type and message as the log, at the same file
- First-error and last-healthy timestamps are recorded from telemetry, not estimated
- Every deploy of the service in the window is listed with version, commit, ticket and time
</verification>
</phase>

<phase name="Test the leads" id="2">
## Test the leads

1. Lead A — latest deploy. Check `git log -L` on the failing line; check out the pre-deploy release into a worktree and replay
   the same live input. If it fails identically, the deploy is ruled out; record the healthy request count on the new version
   between deploy and first error as corroboration. If it passes, the deploy is the cause — bisect within it.
2. Lead B — the input changed. Diff the live input against the last healthy one (shape, schema version, encoding). Look for
   vendor notices, contract repositories, consumer registers and upstream commits around the first-error time.
3. Any further lead (infra, dependency, config) gets the same treatment: a hypothesis, a test, a verdict with evidence.
4. Exactly one lead becomes ROOT CAUSE; every other lead is marked RULED OUT with the test that ruled it out.

<verification>
- The latest deploy was tested by replay on pre-deploy code, not dismissed or accepted on timing alone
- The root-cause verdict cites at least two independent pieces of evidence (e.g. input diff + vendor notice + consumer register)
- Ruled-out leads are kept in the record with their evidence — they are part of the story
</verification>
</phase>

<phase name="Fix on a branch" id="3">
## Fix on a branch

1. Create an `incident/<id>-<slug>` branch. Make the smallest change that handles both the old and new input and fails
   loudly on anything else — never a silent coercion that would turn a 500 into a wrong number.
2. Add a regression test using the live input, an equivalence test (old ≡ new), and an unknown-shape test.
3. Run the repository's test and lint commands. Replay both the live input and the last healthy input.
4. Commit (`bug: <id> …`) and write the PR body: root cause, what changed, what was ruled out and how, verification, what is
   not in this PR. Reviewers from CODEOWNERS. The PR goes to the COG-GTM repo hosting `client-portal` (`scripts/repo-map.tsv`,
   `COG-GTM/traderXCognitiondemos` under `arden/client-portal/`; push with `scripts/publish-branch.sh`). Do not deploy or roll back, never merge.

<verification>
- Tests pass before and after (the new tests fail on the unfixed code)
- Lint passes; live and last-healthy inputs both replay successfully on the branch
- PR body lists the ruled-out lead with its evidence and ends with `Devin-Org: engineering`
- No deploy, rollback, feature-flag or vendor-config change was made
</verification>
</phase>

<phase name="Verify the frontend" id="4">
## Verify the frontend

1. Start the service on the fix branch with the live input (`ARDENFEED_BATCH=… npm start` for client-portal).
2. Open the portal in a browser; load the page that was failing and one that was not; confirm the valuation rows render with
   the expected prices and currencies and that the previously unaffected page is unchanged.
3. Record a screen recording of the walk-through (start on the failing page, then the unaffected page) as proof of no negative
   impact; capture screenshots; stop the server.

<verification>
- The failing page renders on the fix branch; the unaffected page still renders
- Values shown match the replay output (price, currency, market value) for at least one row
- A screen recording and screenshots are saved and referenced from the PR body
</verification>
</phase>

<phase name="Leave-behinds and hand-over" id="5">
## Leave-behinds and hand-over

1. Post (or draft) the incident-channel thread: alert, reproduction, each lead with verdict, fix branch, what is left for humans.
2. Write the timeline (vendor notice → deploys → last healthy → cutover → first error → alert → investigation steps).
3. Write the incident record with the regulatory-classification, client-impact and closure fields explicitly marked as human
   decisions (DORA major-incident criteria; 4h-from-classification / 24h-from-awareness initial notification if major).
4. Write the RCA: what happened, the lead that was wrong and how it was ruled out, root cause, fix, why it was not caught, and
   recommendations each with a named owner. State what the RCA does not decide.
5. Hand over to the CODEOWNERS team for rollout and to ops risk for classification.

<verification>
- The thread, timeline, incident record and RCA all agree on times, versions and root cause
- The incident record leaves classification, client impact and closure to named humans
- Recommendations name an owner each and none is claimed as done
- The thread, timeline, incident record and RCA are attached in the session so the user can read them without opening the repo
</verification>
</phase>

## Specifications
- One root cause, with the alternative(s) ruled out by test rather than by timing
- A fix branch with a regression test that fails on the unfixed code; nothing deployed
- Incident record and RCA whose human-owned fields (classification, client impact, closure, rollout) are marked as such

## Forbidden Actions
- Do not deploy, roll back, toggle feature flags or change vendor/feed configuration
- Do not coerce unknown input shapes silently — a wrong number is worse than a 500
- Do not classify the incident under DORA/SEC/OCC rules or draft the regulatory notification; hand the facts to ops risk
- Do not @-mention people found via git blame; use CODEOWNERS teams and the alert's routing
