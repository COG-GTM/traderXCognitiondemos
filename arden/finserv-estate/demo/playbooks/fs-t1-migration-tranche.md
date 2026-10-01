# T+1 settlement migration — inventory, plan and execute one tranche

## Overview
Move a post-trade estate from T+2 to T+1 for a set of markets on a regulator-set date, one tranche at a time. Every
settlement-cycle assumption is inventoried with file/line citations and CODEOWNERS before anything changes; the tranche is
executed on branches with tests; a golden settlement-instruction file is replayed through the old and new code and every
difference is classified. Business questions (calendar authority, corporate-action ex-dates, dual listings, contractual notice
periods) are escalated to their named owner, never guessed. Only ever create the todo list for the current phase.

## What's Needed From User
- Programme ticket (e.g. `SETTLE-4471`), markets in scope (MICs) and the go-live date (e.g. EU/UK/CH, 2027-10-11)
- Repository list or org, and the tranche to execute (1 = instruction-generation path)
- Golden input file of trades (`trade_id,trade_date,mic`) spanning the cutover, or permission to generate one
- Names of the decision owners for calendars, corporate actions and securities lending

## Reference (all phases)
- Estate helpers: `demo/t1/inventory.py`, `demo/t1/migrate.py`, `demo/t1/parity.py`, `demo/run-demo2.sh`
- Each commit message must contain `feature` or `bug`. Every PR description ends with the line `Devin-Org: engineering`.
- Wire formats are contracts: FIX tag 64/75 stay `LocalMktDate` (`yyyyMMdd`); tag 63 changes with the cycle (`3` = T+2, `2` = T+1).
- Consumers pin the date library through the BOM; check what they actually run before choosing a release base.

<phase name="Inventory" id="1">
## Inventory

1. Search every repository for cycle assumptions: `T_PLUS_2`, `T+2`, `plusDays(2)`, `DATEADD(... 2 ...)`, `SettlTyp`/tag 63 = 3,
   `default_cycle`, scheduler run-times keyed to T+1 morning, FX spot / recall notice periods, ex-date arithmetic.
2. Record each hit with repo, path, line, snippet, kind (hard-coded lag / config / mapping / schedule / doc / test / SQL).
3. Attribute owners from the repo's CODEOWNERS for each file.
4. Assess each file: change it (tranche 1 or 2), leave it and say why (false positive, contract to keep green), or escalate
   (tranche 3) with the decision owner named. Zero rows may remain unassessed.
5. Write `demo2-t1-inventory.md` (citations + owners) and `demo2-t1-migration-plan.md` (tranches, decisions owed).

<verification>
- Every hit has repo/file/line, a snippet and at least one CODEOWNERS owner
- No hit is unassessed; each has a tranche or an explicit "no change" reason
- Tranche 3 rows each name a business decision owner, not an engineer
- The plan lists the go-live date and the markets in scope exactly as the user gave them
</verification>
</phase>

<phase name="Execute the tranche" id="2">
## Execute the tranche

1. Stash the production baseline (the release consumers actually run, per the BOM pin) for the parity harness.
2. Create one branch per repo in dependency order (date library → instruction service → message mappings).
3. Make the minimal change: date-gate the default cycle on the trade date; keep no-arg/legacy entry points returning the
   pre-go-live behaviour and mark them deprecated; drop explicit T+2 pins for in-scope markets; update mappings with an
   `effective_from`; archive the pre-migration mapping and golden sample.
4. Run each repo's own tests, then the downstream consumer contract tests against the new artifact.
5. If a contract test fails, do not weaken it. Record the attempt, find the cause (e.g. an unrelated wire-format change on
   the branch you built from), and take the smallest deviation that keeps the contract green (e.g. cut the release from the
   line consumers run). Record both attempts and the reason in the PR body.
6. Commit per repo (`feature: <ticket> T+1 tranche N — <repo>`) and produce the PR body: what changed, tests, deviation,
   follow-ups, reviewers from CODEOWNERS, ending with `Devin-Org: engineering`.

<verification>
- Every branch builds and its tests pass; the consumer contract test (tag 64 = yyyyMMdd) passes against the new artifact
- Nothing outside the tranche's file list changed
- Any deviation from the first plan is recorded with the failing test output and the reason
- No branch was merged and no BOM pin was moved
</verification>
</phase>

<phase name="Golden-file parity" id="3">
## Golden-file parity

1. Replay the golden file through the baseline and the new artifact with the same CLI/entry point.
2. Diff every row on settlement date, tag 63, tag 64, affirmation deadline.
3. Classify each row: `unchanged` (pre-go-live or already-T+1 market), `expected` (T+2→T+1, tag 63 3→2, tag 64 format
   unchanged), or `ESCALATE` (venue vs cash-calendar disagreement, unexpected change, unchanged in-scope trade).
4. List corporate-action events straddling go-live with the ex-date under both rules; route to the asset-servicing owner.
5. Write `demo2-parity.html` and `demo2-migration-report.md`.

<verification>
- Every golden row appears in the report with old value, new value and a verdict
- No tag 64 value changed format between old and new
- Every ESCALATE row names the decision owner and shows the candidate values
- The report states what was deliberately not done (other tranches, escalated items)
</verification>
</phase>

<phase name="Verify the frontend" id="4">
## Verify the frontend

1. Start `client-portal` from the workspace (`npm ci && npm start`) with the migrated date library on the classpath/branch.
2. Open the portal in the browser, navigate the trade blotter and settlement-status pages, and load a trade dated after
   go-live and one before.
3. Record a screen recording of the navigation; note any wording that still says T+2 for in-scope markets (tranche 2 item,
   do not change it here).

<verification>
- The portal starts and its main pages render without console errors
- Screen recording saved and referenced from the report
- Any stale T+2 wording is listed in the tranche 2 plan, not changed in this tranche
</verification>
</phase>

<phase name="Open PRs and hand over" id="5">
## Open PRs and hand over

1. Open one PR per estate repo on the COG-GTM repo that hosts it (`scripts/repo-map.tsv`, e.g. core-dates → `COG-GTM/Securities-Trade-Processing-System` under `arden/core-dates/`;
   push with `scripts/publish-branch.sh <repo> <branch> <patch>`), reviewers = CODEOWNERS of the changed files, body from phase 2, last line `Devin-Org: engineering`. Never merge.
2. Post the escalation list to the programme channel/ticket, one item per decision owner.
3. Report: `TRANCHE=<n> REPOS="<list>" STATUS=<migrated|migrated-with-deviation|blocked> PARITY="<changed/expected/escalated>" ESCALATIONS=<n>`.

<verification>
- Each PR links the inventory, plan and parity report
- Each PR body ends with `Devin-Org: engineering`
- Escalations are posted to the owners named in the plan, not left in the report only
</verification>
</phase>

## Specifications
- Go-live date and markets come from the user; never infer them from regulatory news.
- Old behaviour is preserved for trade dates before go-live and for markets outside scope.
- FIX tag 64/75 wire format is unchanged; tag 63 follows the cycle.

## Forbidden Actions
- Do not weaken, delete or skip a consumer contract test to get green.
- Do not decide calendar authority, ex-date regime, dual-listing treatment, FX value dates or recall notice periods in code.
- Do not merge, move BOM pins, or change repos outside the tranche's file list.
- Do not run bare `pytest`; use `python -m pytest`.
