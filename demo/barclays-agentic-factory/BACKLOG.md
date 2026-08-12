# Epic TRX-100 — Pre-trade risk controls & best-execution audit trail

> **The business ask, as it actually arrives:**
> "Compliance want us to stop traders putting on positions that blow through account limits,
> and they want to be able to prove afterwards why any given order was accepted or rejected."

That sentence is the entire input. Everything below is what comes *out* of scoping it with
Devin in plan mode — which is the point of the first ten minutes of the demo.

The epic is deliberately shaped so that it:

* crosses **five services and three languages** (nothing here is a single-file change),
* has a **regulatory driver** (MiFID II RTS 6 pre-trade controls, RTS 27/28 best-execution
  record keeping) rather than being invented feature work,
* contains one ticket that is a **bad idea**, so the room can watch an agent push back
  instead of dutifully building the wrong thing.

## Tickets

| ID | Title | Services touched | Role that drives it | Runs in parallel? |
| :--- | :--- | :--- | :--- | :--- |
| [TRX-101](tickets/TRX-101-pre-trade-notional-limit.md) | Pre-trade notional limit check on order submission | trade-service | BA (plan mode) → session | yes |
| [TRX-102](tickets/TRX-102-risk-limit-admin-api.md) | Risk limit storage + admin API | account-service, database | PM fan-out | yes |
| [TRX-103](tickets/TRX-103-ui-rejection-reason.md) | Surface the rejection reason in the trade ticket | web-front-end (Angular + React) | PM fan-out | yes |
| [TRX-104](tickets/TRX-104-best-execution-audit-trail.md) | Immutable best-execution audit record per order decision | trade-service, trade-processor, database | PM fan-out | yes |
| [TRX-105](tickets/TRX-105-audit-query-api-and-blotter-tab.md) | Audit query API + "Compliance" tab on the blotter | position-service, web-front-end | PM fan-out | depends on 104 |
| [TRX-106](tickets/TRX-106-kafka-event-sourcing.md) | "Move trade-feed to Kafka and event-source everything" | everything | **pushback beat — never built** | n/a |

## Dependency shape

```
TRX-102 (limits data) ──┐
                        ├──> TRX-101 (enforcement) ──> TRX-103 (UI reason)
                        │
TRX-104 (audit record) ─┴──> TRX-105 (audit API + tab)
```

101, 102, 103 and 104 are independent enough to run as **four concurrent sessions**; 105 is
queued behind 104 on purpose, so the fleet view shows both parallelism *and* sequencing.

## Acceptance criteria that apply to every ticket

Written once here so each session inherits them, and so the room sees that "context must be
explicit" is a working practice, not a slogan.

1. Existing behaviour is preserved — a trade within limits still books exactly as it does today.
2. New logic is unit tested; the service still builds with `./gradlew build`.
3. No new runtime dependency is introduced without saying why in the PR description.
4. Configuration (limits, feature flags) is externalised, not hard-coded.
5. The feature can be switched off with a single flag, defaulting to **on** in dev.
6. The PR description explains the *regulatory* reason for the change, not just the mechanics.
