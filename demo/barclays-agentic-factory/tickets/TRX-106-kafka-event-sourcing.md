# TRX-106 — "Move trade-feed to Kafka and event-source everything"

**Status: never built. This ticket exists to be argued with.**

**Role beat:** the pushback moment. This is the direct analogue of the Cassandra exchange in
the EDP session, which Steffen has already seen land — repeating the *shape* of it on a
different codebase is what makes it look like a property of the product rather than a lucky
moment.

## The ticket, as a well-meaning architect would write it

> While we're in here adding audit records, let's do it properly: replace the socket.io
> `trade-feed` with Kafka, event-source the whole order lifecycle, and rebuild positions as a
> projection. Should be doable this sprint alongside the risk work.

## What we expect the agent to say

Not "yes". Plan mode should come back with some version of:

* The audit requirement in TRX-104 does **not** need an event store. Append-only rows in the
  existing database satisfy RTS 27/28 record keeping; event sourcing is a much larger bet
  being smuggled in under a compliance deadline.
* It collides with TRX-101/104, which are being built concurrently against the current
  `Publisher`/`Subscriber` abstraction — merge conflicts across five services, and the
  regulatory work becomes blocked on an architecture migration.
* Positions-as-projection changes the read model the blotter and `position-service` depend on;
  that is its own epic with its own testing story, not a line item in this one.
* Operationally, Kafka is new infrastructure — brokers, topics, retention, DR, a team to run
  it. TraderX's own design goal is to run on a laptop with no assumptions beyond Node, Java
  and Python. Nothing in the stated requirement justifies breaking that.
* A reasonable counter-proposal: keep the current bus, make the audit write synchronous and
  append-only now, and if event sourcing is genuinely wanted, spike it separately behind the
  existing `Publisher` interface — which is already the seam you'd migrate on.

## How to run this beat

Paste the ticket into **plan mode**, live, and read the response out. Do not pre-run it and
show a screenshot — the value is that the room watches it happen to a prompt they just heard.

If the room wants to test it, invite Steffen to write the bad ticket himself. It is a better
demo when the prompt isn't yours.

> **Presenter note:** the point to land afterwards is not "the agent is clever". It is that
> **ambiguity escalates to humans** and **scope is bounded before execution starts** — which
> is exactly the control an agentic factory needs if it is going to run outside a small
> high-performing team.
