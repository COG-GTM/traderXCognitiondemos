# TRX-103 — Surface the rejection reason in the trade ticket

**Service:** `web-front-end` (Angular, and React where it already supports trading)
**Role beat:** PM fan-out, session 3 of 4 — and the PR the **developer** reviews on stage.

## Context an agent needs

The trade ticket posts to `trade-service` `POST /trade/`. Today any failure is effectively a
404 with a string body, so the UI can only say "something went wrong". TRX-101 introduces a
structured 422 with `decision`, `reason`, `limit` and `attempted` fields.

There are two front ends in `web-front-end/`. The Angular app is the original, feature-complete
one including account management; the React app was a hack-day contribution and does trading
and the blotter but not accounts. Both should handle the new response; do not "fix" the React
app's missing account management as a side quest.

## What we want

* On a 422, the trade ticket shows an inline, non-modal error naming the breach in language a
  trader would accept: the limit, what they attempted, and the shortfall.
* The order stays in the form so it can be amended and resubmitted — do not clear it.
* A generic failure still shows the existing generic message; the new path must not swallow
  unrelated errors.
* No new UI dependency. Use what the app already has.

## Why this is the review beat

This is a small, visual, opinionated change — exactly the kind where a human reviewer has
taste the agent should defer to. Expect to leave a review comment on stage (wording, placement,
or that the shortfall should be formatted as currency) and have it addressed without a human
opening an editor.

## Definition of done

* Angular app builds; React app builds.
* The error path is exercised by a test or, failing that, demonstrated in the PR with a screenshot.

## Paste-ready prompt

> Implement TRX-103 from `demo/barclays-agentic-factory/tickets/TRX-103-ui-rejection-reason.md`
> in COG-GTM/traderXCognitiondemos. Follow the epic-wide acceptance criteria in
> `demo/barclays-agentic-factory/BACKLOG.md` and open a PR against `main`.
