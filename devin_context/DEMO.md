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
than a wall of nitpicks. And it's allowed to tell you the *doc* is wrong; when we ran this, several
findings were bugs in the docs rather than the code, and the docs got fixed.

**The line to use here.** Its second finding was a real bug, live in TraderX today: both blotters
register `getRowId` as `` `Trade-${id}` `` but look rows up with the bare `data.id`, so every
trade-feed message misses and takes the *add* branch. The `New → Processing → Settled` flash the
app is built around had never worked, and rows duplicated on update. One line each to fix (commit
`8cb5617`). It found that by reading a *convention* about `applyTransaction`, not by running a
linter — no linter has an opinion about this, and in fact `npm run lint` in this repo has been
dead since Angular 12, which the audit also caught.

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

Every beat below was actually run against this branch. Cut to the artifact if a beat runs long.

| Beat | Session | Artifact |
| --- | --- | --- |
| 1 — context | — | [PR #90](https://github.com/COG-GTM/traderXCognitiondemos/pull/90) |
| 2 — audit | [095… audit](https://app.devin.ai/sessions/7a6c73555304445fbd8099346a6c7cea) | 12 findings ranked by blast radius; no code changed |
| 3 — feature | [Positions tab](https://app.devin.ai/sessions/6041ffa7aea24f8a850bc960b879712b) | [PR #92](https://github.com/COG-GTM/traderXCognitiondemos/pull/92) + recording |
| 4 — from mock | [blotter summary](https://app.devin.ai/sessions/e52991956d364e7f9244b3a19c9085c8) | [PR #93](https://github.com/COG-GTM/traderXCognitiondemos/pull/93), mock vs. screenshot in the body |

What each build session came back with, worth knowing before you narrate it:

- **Positions tab (#92)** — 40 tests (was 25), verified live: three tickets booked on the Trade tab
  while an untouched Positions window watched them land. It also reported that `UPDATED` is always
  blank because `trade-processor` never calls `position.setUpdated(...)` — a backend gap it
  refused to paper over.
- **From the mock (#93)** — 42 tests, four cards not five: *"no service or model supplies price,
  notional or valuation, so it's omitted rather than faked."* It also found the mock's `Pending`
  filter can never match, because the H2 check constraint and the processor's `TradeState` enum
  only allow `New/Processing/Settled/Cancelled`. Built as specced, flagged for a decision — that's
  the behaviour you want on a comp someone drew from memory.

Caveat if you run any of this live: `docker compose up --build` currently fails on the Java
services (Maven Central 429s). Both build sessions worked around it with the published
`ghcr.io/finos/traderx/*` images plus `npm --prefix web-front-end/angular start` on `:18093`.

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
