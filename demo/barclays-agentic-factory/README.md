# Barclays IB — Agentic Software Factory Demo (TraderX)

A 45-minute, live, feature-development demo built on FINOS TraderX. The question it answers
is not "can an agent write code?" — it is **"what does each role do differently the next
morning?"** for the BA, the PM, the developer and the team lead.

Audience: Steffen (Barclays Investment Bank), scoping an agentic software factory —
build vs. buy vs. partner. He wants implementation that runs **off-machine**, outside a
small high-performing team, at scale, with requirements genuinely separated from
implementation.

| File | What it is |
| :--- | :--- |
| [RUN_OF_SHOW.md](RUN_OF_SHOW.md) | Minute-by-minute script, exact prompts, fallback links |
| [BACKLOG.md](BACKLOG.md) | The epic and its six tickets, as a BA would hand them over |
| [tickets/](tickets) | One file per ticket — paste-ready session prompts |
| [ROLE_CHANGES.md](ROLE_CHANGES.md) | Before/after for each role + the RACI Steffen can take to his org |
| [OBJECTIONS.md](OBJECTIONS.md) | Prepared answers to the questions this room will actually ask |

## Why TraderX

TraderX is FINOS's own reference trading application — an investment-bank-shaped estate in
miniature, and one no one in the room can wave away as a toy:

* **Polyglot, exactly like the real thing** — Java/Spring (`trade-service`, `account-service`,
  `position-service`, `trade-processor`), .NET (`people-service`), Node/NestJS
  (`reference-data`), Node/socket.io (`trade-feed`), Angular **and** React front ends, H2/SQL.
* **Distributed** — REST plus a message bus, so a single feature genuinely crosses service
  and language boundaries. That is the whole point: it is where "just use Copilot" stops working.
* **Neutral ground** — it is not the EDP proof of concept Steffen has already said "has some
  ways to go to prove value", and it is not a Barclays repo, so nothing is off-limits on screen.

## The demo in one line

One epic — **pre-trade risk controls and a best-execution audit trail** — taken from a crude
one-sentence request to merged, reviewed pull requests across five services, with a different
human role in the driving seat at each stage.
