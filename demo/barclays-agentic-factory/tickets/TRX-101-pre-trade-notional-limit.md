# TRX-101 — Pre-trade notional limit check on order submission

**Service:** `trade-service` (Java 21 / Spring Boot)
**Regulatory driver:** MiFID II RTS 6 Art. 15 — pre-trade controls on order value and volume.
**Role beat:** the BA. This is the ticket we scope live, in plan mode, from one crude sentence.

## Context an agent needs

`TradeOrderController.createTradeOrder` currently validates two things before publishing to
the `/trades` topic: the ticker exists in `reference-data`, and the account exists in
`account-service`. Both validations are inline private methods using a raw `RestTemplate`,
and both throw `ResourceNotFoundException` on failure — so today, *every* rejection looks
like a 404 "not found", which is useless to a trader and worse to a compliance officer.

## What we want

Reject an order **before** it is published when its notional value would breach the limit
configured for that account, and reject it in a way that says *why*.

* Notional = `quantity × last price` for the security. Reference data is the price source;
  if no price is available the order is rejected as un-priceable rather than waved through.
* Limits come from `account-service` (see TRX-102). Until that lands, read them from
  configuration with a sane default.
* A breach returns **422 Unprocessable Entity** with a structured body:
  `{ "decision": "REJECTED", "reason": "NOTIONAL_LIMIT_BREACH", "limit": …, "attempted": … }`
  — not a 404, and not a bare string.
* Buy and sell are both in scope. Absolute notional, no netting against existing positions
  (netting is a follow-up; say so rather than quietly implementing it).
* Behind flag `traderx.risk.pre-trade-checks.enabled`, default `true`.

## Deliberate ambiguities — we want these surfaced, not guessed

These are in the ticket on purpose. A good plan asks about them; a bad agent picks one and
ships. This is the moment the room sees the difference.

* What happens to an order that breaches the limit *because* of an in-flight order that has
  not yet been processed?
* Is the limit per-order or per-day cumulative?
* Who is allowed to override a breach, and is an override itself an auditable event?
* Should `trade-processor` re-check the limit at processing time, or is a single check at
  submission enough given the message bus is asynchronous?

## Definition of done

* `RiskLimitService` (or equivalent) is a separate, injectable, mockable class — the existing
  inline validation style is explicitly *not* the pattern to copy. The current code even
  carries a `// Move whole method to a separate class ...` comment; honour it.
* Unit tests cover: within limit, exactly at limit, over limit, missing price, flag disabled.
* `./gradlew :trade-service:build` passes.

## Paste-ready prompts

**Plan mode (the BA beat — read-only, no code):**

> Compliance want to stop traders breaching account limits, and they want to prove afterwards
> why an order was accepted or rejected. Scope this for TraderX and tell me what you'd need
> from me before you'd be willing to build it.

**Session (the execution beat):**

> Implement TRX-101 from `demo/barclays-agentic-factory/tickets/TRX-101-pre-trade-notional-limit.md`
> in COG-GTM/traderXCognitiondemos. Follow the epic-wide acceptance criteria in
> `demo/barclays-agentic-factory/BACKLOG.md` and open a PR against `main`.
