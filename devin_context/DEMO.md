# Demo runbook — "the repo carries its own frontend conventions"

**Claim:** you don't get consistent UI out of an agent by writing longer prompts. You get it by
putting the conventions in the repo, versioned next to the code, and pointing at them.

~11 minutes, 5 beats. Every beat has a pre-baked artifact (see the table at the bottom) so you can
cut to the result if the room is short on time.

---

## Beat 1 — the context folder (2 min)

Open `devin_context/` on screen.

> "This is a real FINOS reference app — Angular 18, Bootstrap 5, AG Grid, a socket.io trade feed.
> Nothing here was invented for the demo: every rule was reverse-engineered from the code."

Show, in this order:

1. `README.md` — the **golden files** table. "New screen? Mirror `trade.component.ts`."
2. `frontend/design-system.md` — "the design system is five CSS overrides and a button-semantics
   table. `btn-primary` primary, `btn-info` row action, `btn-secondary` cancel, all `btn-sm`. That
   table is the difference between a screen that fits and one that doesn't."
3. `frontend/data-and-state.md` — scroll to **What the API does *not* give you**. "No price, no
   P&L anywhere in TraderX. Remember that one."
4. `../AGENTS.md` — "and it's wired in here, so every session picks it up without being told."

## Beat 2 — audit before you build (2 min)

Run `prompts/frontend-audit.md`. Read-only.

> "Before it writes anything, it can tell you where the existing UI already drifted."

Talking point when the report lands: it ranks by blast radius, quotes the rule and the doc that
states it, and lists what it checked and found **clean** — that's what makes it an audit rather
than a wall of nitpicks. And it's allowed to tell you the *doc* is wrong; when we first ran this,
some of its findings were bugs in the docs, not the code, and we fixed the docs.

## Beat 3 — a feature, from the conventions alone (3 min)

Run `prompts/frontend-feature.md` (Positions tab).

> "No design attached. Just 'follow the conventions' — and the shape of the result is decided by
> the repo, not by how well I wrote the prompt."

What to point at in the diff: feature folder + module + route + header tab (the four-file rule),
container/presentational split, `ColDef[]` in the class, `applyTransaction` for feed updates, the
subscription teardown called in `ngOnDestroy`, a spec using the existing mock services, `id`s on
every control — and `package.json` untouched.

## Beat 4 — a screen from a mock (3 min)

Open `design/blotter-summary.png`, then run `prompts/frontend-from-mock.md`.

> "Same folder, one more artifact: a comp, and a spec beside it written in the same vocabulary as
> the conventions. So 'match the design' and 'follow our standards' are one instruction."

**The trap — set it up before the result lands.** The mock has five summary cards. The fifth is
*Unrealized P&L, +$12,480*. TraderX has no prices anywhere, so that number cannot exist. The right
outcome is four cards and a note in the PR saying why the fifth was dropped — not an invented
figure. This is the beat that lands with risk and control functions.

*Variant, if you have a willing audience:* drop **their** screenshot into `design/` live and run
the same prompt against it.

## Beat 5 — review (1 min)

Open a PR and scroll to the pasted `frontend/review-checklist.md`, then the mock and the
screenshot side by side.

> "The checklist is in the repo, so review is the same conversation every time — and most of it is
> already answered before a human opens the diff."

---

## Pre-baked results

| Beat | Artifact |
| --- | --- |
| 1 — context | this branch / the `devin_context/` PR |
| 2 — audit | _session + report attached below once run_ |
| 3 — feature | _PR link_ |
| 4 — from mock | _PR link, mock vs. screenshot_ |

## Setup

```bash
docker compose up      # whole stack, UI on http://localhost:8080
```

or the UI alone against running services:

```bash
npm --prefix web-front-end/angular install
npm --prefix web-front-end/angular start        # :18093
```

No login: pick an account from the dropdown on the Trade tab. To show live behaviour, create a
trade ticket and watch the row flash `New → Processing → Settled` in the blotter.
