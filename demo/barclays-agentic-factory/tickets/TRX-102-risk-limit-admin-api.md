# TRX-102 — Risk limit storage + admin API

**Services:** `account-service` (Java 21 / Spring Boot), `database` (H2)
**Regulatory driver:** MiFID II RTS 6 Art. 15 — limits must be set, owned and changeable by
a function independent of the trading desk.
**Role beat:** PM fan-out. Session 2 of 4.

## Context an agent needs

`account-service` already owns accounts and is already the service `trade-service` calls to
validate an account exists. The H2 schema lives under `database/`. Adding limits here keeps
the enforcement point (trade-service) separate from the authority that sets the limit, which
is the whole compliance argument.

## What we want

* A `risk_limit` table keyed on account, holding at minimum: max order notional, currency,
  effective-from timestamp, and who set it.
* `GET /account/{id}/risk-limit` — used by `trade-service` at enforcement time.
* `PUT /account/{id}/risk-limit` — used by the risk function to set or amend a limit.
* Amending a limit is itself a recorded event: never overwrite in place without leaving a
  trail of the previous value and who changed it.
* Seed sensible limits for the existing demo accounts so the demo has something to breach —
  one account tight enough that a plausible-looking order trips it on stage.

## Deliberate ambiguities

* Should a missing limit mean "unlimited" or "reject everything"? (Compliance would say the
  second; the demo needs the first to not break existing flows. We want this raised.)
* Per-account, per-trader, or per-desk? The ticket says account; ask whether that is really
  what compliance meant.

## Definition of done

* Liquibase/SQL change is additive — existing data and flows are untouched.
* Unit tests cover get, put, amend-history, and missing-limit behaviour.
* `./gradlew :account-service:build` passes.

## Paste-ready prompt

> Implement TRX-102 from `demo/barclays-agentic-factory/tickets/TRX-102-risk-limit-admin-api.md`
> in COG-GTM/traderXCognitiondemos. Follow the epic-wide acceptance criteria in
> `demo/barclays-agentic-factory/BACKLOG.md` and open a PR against `main`.
