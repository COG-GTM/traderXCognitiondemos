# Prompt: audit the UI against our conventions (read-only)

Copy from the line below into a Devin session on this repo.

---

Read `devin_context/README.md` and every doc under `devin_context/frontend/`. They are the
normative frontend conventions for this repo. Then audit `web-front-end/angular/main` against
them.

This is **read-only**: do not change any code, do not open a PR, do not run a formatter.

For every violation report:

1. `path/to/file.ts:LINE`
2. the exact rule it breaks, quoted from the doc that states it (name the doc)
3. blast radius — how many files/screens share the problem, and what breaks in practice
4. the one-line fix

Rank findings by blast radius, not by file order. Group repeats into one finding with a file list
rather than repeating yourself.

Pay particular attention to:

- dependency and framework drift (anything outside the sanctioned set; standalone/new-syntax
  Angular creeping in)
- components doing their own I/O instead of going through `main/app/service/`
- socket.io subscriptions whose teardown is never called
- AG Grid rows replaced wholesale instead of `applyTransaction`
- hard-coded colours, sizes or inline styles outside the AG Grid sizing exception
- missing or non-conforming `id`s on interactive controls and grids
- components without a spec, and specs that hit the network
- dead or unreachable UI code

Then list what you checked and found **clean**, so the report is a real audit rather than a list of
grievances. If one of the docs is wrong about the code — the code is right and the doc is stale —
say so explicitly; that's a finding too.

Deliver the report as a markdown file attached to your response.
