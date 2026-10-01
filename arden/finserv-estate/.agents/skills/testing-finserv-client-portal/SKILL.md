---
name: testing-finserv-client-portal
description: Browser verification of the local financial-services demo client-portal incident fix and quote-schema compatibility.
---

# Client portal incident verification

This generated estate is independent of the event-driven-devin app. Read the
current `client-portal/src/server.js` before assuming there is an HTML frontend.
The current service is API-only: open `/api/portfolio/valuation`,
`/api/settlement/summary`, and `/health` in Chrome. Use Chrome's Pretty-print
checkbox to make JSON readable; Ctrl+= enlarges it.

## Setup

- Coordinate with other agents: estate reset/reseed commands replace workspace
  repositories and may invalidate both branch hashes and running processes.
- Verify branch, HEAD, and clean status before starting. After the incident runner,
  expect the incident branch; use `main` as the pre-fix reference rather than a
  hard-coded commit hash, since reseeding regenerates history.
- Run from the client-portal repository root so relative fixture paths resolve.
- Node >=20 is required; the service has no package dependencies.
- Start with `PORT=3200 ARDENFEED_BATCH=fixtures/ardenfeed-v2.3.json node src/server.js`.
- For before evidence, use an explicitly authorized temporary Git worktree of
  `main`, a different port, and the same v2.3 fixture. Do not stash or reset the
  working checkout.
- Start another process with `fixtures/ardenfeed-v2.2.json` for compatibility.
  Separate processes prevent the module-level valuation cache mixing fixtures.

## Evidence

Record actual Chrome navigations. The pre-fix response exposes only
`{"error":"valuation_failed"}`; corroborate the HTTP 500 and precise TypeError
using the Node process output for that browser request. The error stack is not
displayed in the JSON response.

Compare all rows against fixture prices and position quantities, including
currency and timestamp. Label settlement and legacy-fixture checks Regression.
The API-only server may log an expected 404 for Chrome's `/favicon.ico`.

Stop every process started for testing, remove only the temporary worktree, and
verify original branch/HEAD and clean status are unchanged.

## Devin Secrets Needed

None for local fixture-backed browser verification.
