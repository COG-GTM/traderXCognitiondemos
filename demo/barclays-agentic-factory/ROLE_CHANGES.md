# What each role does differently the next morning

Steffen's literal ask: *take a real application, plug it into Devin, show how each role
changes the next day.* This is the takeaway artefact for that question — the demo is the
evidence, this is the summary he can forward inside the bank.

## Business analyst

| Yesterday | Tomorrow |
| :--- | :--- |
| Writes a spec, then defends it in refinement for two weeks | Interrogates a spec that already knows the codebase, in ten minutes |
| Discovers cross-service impact when a developer complains | Gets the impact map before the ticket is agreed |
| Ambiguity is resolved by whoever picks the ticket up, silently | Ambiguity is listed, in writing, before anyone builds |

The BA becomes the highest-leverage role in the chain, because the quality of the scoped spec
now determines the quality of the output directly, with nothing in between to absorb it.

## Product / project manager

| Yesterday | Tomorrow |
| :--- | :--- |
| Capacity = headcount × velocity, discovered retrospectively | Capacity = how many well-scoped tickets are in the queue |
| Status is what people say in stand-up | Status is observable per session, continuously |
| Parallelism is limited by who is free | Parallelism is limited by genuine dependencies |

The PM's job shifts from chasing progress to **sequencing and unblocking**. The backlog stops
being a wishlist and becomes an input queue — which also means a badly-groomed backlog now
has an immediate, visible cost.

## Developer

| Yesterday | Tomorrow |
| :--- | :--- |
| Writes the diff | Reviews the diff, and owns whether it is right |
| Context lives in their head and their laptop | Context is written down or the agent doesn't have it |
| Specialises by service or language | Reviews across the estate; more full-stack by default |
| Interrupted constantly by small tickets | Small tickets are queued, not context-switched into |

This is the beat that most often lands badly if you oversell it. The honest version: the
routine tier of the work moves, the judgement stays, and the developer's leverage goes up
because they are now reviewing five changes in the time they used to write one. The skill
that appreciates is **packaging context**; the skill that depreciates is typing the
implementation of a well-understood change.

## Team lead / engineering manager

| Yesterday | Tomorrow |
| :--- | :--- |
| Standards live in a wiki nobody reads | Standards live in playbooks and knowledge, applied every time |
| Review is the bottleneck as volume rises | Automated review triages; humans arbitrate |
| Onboarding a new joiner takes a quarter | The estate's knowledge is written down and queryable on day one |
| Institutional knowledge leaves when people do | Institutional knowledge accrues in the platform |

The lead's job becomes **setting the constraints once** rather than enforcing them
repeatedly — and their real deliverable becomes the team's context layer.

## The operating principles to state out loud

These are what make it governable, and a room scoping a factory cares more about these than
about throughput:

1. Humans own outcomes. Agents own bounded execution.
2. Review is mandatory. Nothing ships on agent confidence.
3. Context must be explicit — if it isn't written down and attached, the agent doesn't know it.
4. Ambiguity escalates to humans. Uncertain agents stop and ask rather than guess and ship.
5. Destructive permissions stay constrained. Least privilege is a hard rule.
6. Coherence beats local speed.

## RACI, for the slide he'll ask for

R = responsible · A = accountable · C = consulted · I = informed

| Step | BA / PM | Developer | Team lead | Agent |
| :--- | :--- | :--- | :--- | :--- |
| Define the outcome | R/A | C | C | I |
| Package the context | R | A | C | I |
| Draft the implementation plan | C | A | C | R |
| Break into tasks | C | A | C | R |
| Implement | I | A | I | R |
| Review correctness | C | R/A | C | I |
| Validate in reality | C | R/A | C | — |
| Merge and release | C | R/A | A | — |

The agent is never *accountable* for anything. That column is the answer to the governance
question before it is asked.
