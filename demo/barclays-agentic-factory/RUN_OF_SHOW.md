# Run of show — 45 minutes

Repo on screen: **COG-GTM/traderXCognitiondemos** (fork of FINOS TraderX).
Everything below is live unless marked *(fallback)*.

**The through-line, stated at the top and again at the end:** one feature request, four
roles, nobody's laptop. Steffen's framing was "true separation of requirements from
implementation" — so the demo is organised by *who is doing what*, not by *what the agent can do*.

---

## 0:00 — 0:03 · Frame it, then get out of the way

One slide or no slide. Say this:

> "Last time you saw a migration. You asked what happens to the day job — so today is a
> feature, on a codebase neither of us owns, and I'm going to run it as four different people.
> Watch who is doing the work at each stage."

Put the epic on screen: `demo/barclays-agentic-factory/BACKLOG.md`. One sentence of business
requirement, six tickets, five services, three languages.

---

## 0:03 — 0:13 · The BA · *from a sentence to a scoped spec*

**Beat 1 — understanding before scoping (2 min).**
Open the repo's indexed understanding — the architecture diagrams and service map. The line
to use: *"nobody read this repo this morning. It has been read continuously since we indexed
it, and what it learns persists."* Tie back to your day-1/day-30 framing from the first call.

**Beat 2 — plan mode on a crude prompt (6 min).** Paste, verbatim:

> Compliance want to stop traders breaching account limits, and they want to prove afterwards
> why an order was accepted or rejected. Scope this for TraderX and tell me what you'd need
> from me before you'd be willing to build it.

While it works, narrate what a BA does with this: they are no longer writing the spec, they
are **interrogating** one. Two things to point at when it comes back:

* `trade-service` returns a 404 for every kind of rejection today — bad ticker, unknown
  account, all of it. Nobody had written that down anywhere.
* **There is no price data in TraderX at all.** "Stop traders breaching limits" implies a
  notional, a notional implies a price, and this estate has none. That is the kind of thing a
  BA usually finds out three weeks into the sprint. Here it surfaces before the ticket is
  agreed — and it is the single best moment in the demo, so leave room for it.

**Beat 3 — the pushback (5 min).** Paste TRX-106 (Kafka/event-sourcing) into plan mode. Read
the refusal out loud. Land the principle: *ambiguity escalates to humans; scope is bounded
before execution begins.* If Steffen is engaged, hand him the keyboard and let him write the
bad ticket.

> **What changed for the BA:** the spec is now a conversation with something that has read
> every line of the estate, and the requirement is separated from implementation *by
> construction* — the plan is a written artefact that exists before a single line is written.

---

## 0:13 — 0:22 · The PM · *fan-out and the fleet view*

Kick off **four sessions at once** — TRX-101, 102, 103, 104 — each with its paste-ready prompt
from `tickets/`. Then queue TRX-105 behind 104 and say why: 105 depends on 104's schema, and
a factory that races four agents into the same files is not a factory.

Then stop talking about the code and talk about the board. This is the PM's new instrument
panel: what is in flight, what is blocked, what needs a human, and what it costs. The
comparison to make out loud is with a sprint board where every card is "in progress" and the
truth is in someone's head.

*(fallback)* If the room's network or the clock is against you, the four PRs are already
open — see **Fallback links** below. Show those instead and keep the narration identical.

> **What changed for the PM:** they are sequencing and unblocking, not chasing. Capacity
> stopped being headcount and became how many well-scoped tickets they can put into the queue.

---

## 0:22 — 0:33 · The developer · *review, not authorship*

Open the **TRX-103** PR (UI rejection reason) — deliberately the small, visual, opinionated one,
because that is where a human's taste genuinely beats the agent's.

1. Read the PR description: it explains the regulatory reason, not just the diff.
2. Leave a real review comment on stage. Good candidates: the shortfall should be formatted as
   currency, or the error belongs inline rather than at the top of the form.
3. Have it addressed — no editor opened, no local checkout, no laptop.
4. Then run the app and put a breaching order through the ticket, so the room sees the
   rejection reason in a browser rather than in a diff.

**The tiering beat (2 min).** Switch to Devin for Desktop for one small synchronous change on
the same repo. This is where your three-tier framing from the first call pays off: autocomplete,
synchronous agentic IDE, asynchronous fleet — *and the same understanding of the codebase
underneath all of them.* Steffen is scoping build-vs-buy; the thing that is hard to build is
not the agent, it is the shared context layer.

> **What changed for the developer:** the unit of work is a reviewed diff, not a keystroke.
> The skill that matters is judgement about correctness and taste — and, increasingly, the
> quality of the context they package up front.

---

## 0:33 — 0:41 · The team lead · *quality at volume, and institutional memory*

**Beat 1 — Devin Review (4 min).** Five PRs landed in twenty minutes; nobody's team reviews
that by hand. Show Devin Review's findings on the risk PRs. Be honest about what it catches
and what it doesn't — Steffen has already told you EDP "has some ways to go", so credibility
is the scarce resource in this room. A hedged claim you can back beats a confident one you can't.

**Beat 2 — the compounding asset (4 min).** Show the knowledge notes and the playbook this
epic produced. The argument: after this session the estate knows that rejections must be
structured not 404, that audit records are append-only, that limits are owned by
`account-service`. The next ticket inherits all of it. **This is the part a build-it-yourself
programme cannot shortcut** — you can buy or build an agent, but the institutional knowledge
only accrues by running it, and it starts accruing on day one.

> **What changed for the team lead:** they set standards once, in writing, and they are
> enforced on every PR — rather than being re-litigated in review, inconsistently, forever.

---

## 0:41 — 0:45 · Close on his actual question

Steffen's question is build vs. buy vs. partner, at scale, off-machine. Close on the three
things he can only get from the last 40 minutes:

1. **Off-machine by default.** Every session ran in its own ephemeral micro-VM with its own
   shell, browser and IDE. Nothing touched a developer's laptop, which is what makes it
   auditable and what makes it scale past a small high-performing team.
2. **Requirements separated from implementation.** The spec, the pushback and the plan were
   all artefacts, produced before code and reviewable by people who don't write code.
3. **It compounds.** Day one is ~40%. The reason to start on a real backlog now rather than a
   pilot later is that the clock on institutional knowledge only starts when you begin.

Then the ask: *pick one real backlog — ideally one that is regulatory-driven and boring —
and let's run this against it for thirty days.* Offer to do the next session on a Barclays
repo rather than TraderX, and mention that Paul Sampat's team is already through the
approvals conversation on the GitLab side.

---

## Fallback links

All five were pre-run from exactly the prompts in `tickets/`, so if the live fan-out stalls you
can show these and keep the narration identical.

| Ticket | Pre-run PR |
| :--- | :--- |
| TRX-101 | [#97 — Pre-trade notional limit check](https://github.com/COG-GTM/traderXCognitiondemos/pull/97) |
| TRX-102 | [#98 — Risk limit storage and admin API](https://github.com/COG-GTM/traderXCognitiondemos/pull/98) |
| TRX-103 | [#100 — Surface the rejection reason in the trade ticket](https://github.com/COG-GTM/traderXCognitiondemos/pull/100) |
| TRX-104 | [#99 — Immutable best-execution audit record](https://github.com/COG-GTM/traderXCognitiondemos/pull/99) |
| TRX-105 | [session](https://app.devin.ai/sessions/76a0a2788b644b68b553c9b0068b33b1) — queued behind #99, as designed |

The demo pack itself is [#96](https://github.com/COG-GTM/traderXCognitiondemos/pull/96).

**Worth showing on stage:** each of these PRs has a "Decisions and open questions" section
listing the ambiguities the ticket planted and what the agent did with them. That is the
cleanest evidence for the *ambiguity escalates to humans* principle — better than saying it.

Reusable playbook for this workflow: **TraderX — Feature Delivery Workflow**
(`playbook-47eea92ae5fa40e2b455ba37a6d883c7`, macro `!traderx_feature`). Worth opening during
the team-lead beat as the artefact that encodes the standards.

## Pre-flight checklist

* [ ] Repo indexed and up to date; architecture diagrams render.
* [ ] TraderX running locally (`docker compose up`, as per the repo README) with a browser tab already on the blotter —
      do not let the room watch a Gradle build.
* [ ] An account seeded with a limit tight enough that a plausible order breaches it.
* [ ] The four pre-run PRs open, plus the Devin Review comments already on them.
* [ ] Plan-mode prompts for TRX-101 and TRX-106 in the clipboard/notes app, not typed live.
* [ ] Devin for Desktop open on the same repo, signed in, on a second desktop.
* [ ] Knowledge notes and the playbook visible in a tab.

## Things not to do

* Don't demo the migration story again. He has seen it, and he asked for the day job.
* Don't hide the failures. If a session takes a wrong turn, show the recovery — a room
  scoping a factory is buying the failure mode, not the happy path.
* Don't claim a number you can't source. "~40% on day one" is your framing from call one;
  keep it as framing, not as a benchmark.
