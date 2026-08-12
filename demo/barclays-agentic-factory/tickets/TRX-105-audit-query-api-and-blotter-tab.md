# TRX-105 — Audit query API + "Compliance" tab on the blotter

**Services:** `position-service`, `web-front-end`
**Depends on:** TRX-104
**Role beat:** the queued session — proof that the fleet respects dependencies rather than
racing four agents into the same files.

## Context an agent needs

`position-service` is what the blotter already reads from, so the query side of the audit
trail belongs there rather than in `trade-service`, which should stay on the hot path of
order submission.

## What we want

* `GET /audit/decisions` with filters for account, security, decision and time range, paged.
* A "Compliance" tab alongside the existing blotter tabs, listing decisions with the rejected
  ones visually distinct — because the rejections are the ones a regulator asks about.
* Read-only. No mutation endpoints, no delete button, no "clear" action.

## Definition of done

* Query is paged and indexed; a five-year table is not something you `SELECT *` from.
* Tab renders with an empty state before any orders have been submitted.
* Builds pass for the touched services.

## Paste-ready prompt

> Implement TRX-105 from `demo/barclays-agentic-factory/tickets/TRX-105-audit-query-api-and-blotter-tab.md`
> in COG-GTM/traderXCognitiondemos. TRX-104 is a prerequisite — build on its schema rather
> than inventing a parallel one. Follow the epic-wide acceptance criteria in
> `demo/barclays-agentic-factory/BACKLOG.md` and open a PR against `main`.
