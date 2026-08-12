# TRX-104 — Immutable best-execution audit record per order decision

**Services:** `trade-service`, `trade-processor`, `database`
**Regulatory driver:** MiFID II RTS 27/28 and Art. 16(6) record keeping — the firm must be
able to reconstruct, after the fact, why any given order was accepted or rejected, and
retain that record for five years.
**Role beat:** PM fan-out, session 4 of 4.

## Context an agent needs

Order flow today: `trade-service` validates, publishes to `/trades` on the socket.io
`trade-feed`, and `trade-processor` consumes and books it. There is currently **no record of
a rejection at all** — a rejected order simply never exists. That is the compliance gap, and
it is worth saying out loud in the demo: the interesting audit record is the one for the
order that *didn't* happen.

## What we want

An append-only `order_decision_audit` record written for **every** submission, accepted or
rejected, containing at minimum:

* order identity, account, security, side, quantity, computed notional and the price used;
* the decision (`ACCEPTED` / `REJECTED`) and machine-readable reason;
* the limit that was evaluated and its effective-from timestamp, so the record is
  reconstructable even after the limit is later amended;
* submitting user and a UTC timestamp with millisecond precision;
* a correlation id that ties the submission through to the booked trade in `trade-processor`.

Append-only means append-only: no update path, no delete path. If the code makes it possible
to mutate a record, it has failed the ticket.

## Deliberate ambiguities

* Should the audit write be synchronous with the decision (slower, but no lost records) or
  published to the bus (faster, but the audit trail can drop messages)? For a regulatory
  record the trade-off matters — we want it argued, not assumed.
* Retention: five years is the regulatory answer. Does that belong in this ticket, or is it
  an infrastructure concern to raise and defer?

## Definition of done

* Additive schema change; existing flows untouched.
* Unit tests cover an accepted decision, a rejected decision, and correlation through to
  processing.
* `./gradlew build` passes for both services.

## Paste-ready prompt

> Implement TRX-104 from `demo/barclays-agentic-factory/tickets/TRX-104-best-execution-audit-trail.md`
> in COG-GTM/traderXCognitiondemos. Follow the epic-wide acceptance criteria in
> `demo/barclays-agentic-factory/BACKLOG.md` and open a PR against `main`.
