# TechFest scenario: API-contract migration of a positions consumer

**Synthetic demonstration.** FINOS TraderX is a reference trading application; the account, positions and
market values here are fabricated. Nothing in this directory is customer software or customer data.

| Artifact | Purpose |
|---|---|
| `CONTRACT_BRIEF.md` | Intent and constraints: v1 -> v2 mapping, units, null and aggregation rules. Shown first. |
| `TASK_PROMPT.md` | Copy/paste prompt for the fresh remediation session. Symptoms and constraints only. |
| `ANSWER_KEY.md` | **Presenter only.** Expected solution, wrong-answer table, approved numbers. Not referenced by the prompt. |
| `fixtures/` | Approved fixtures for account **77007**: `v1-positions-77007.json`, `v2-pages-77007.json`, `report-expectations.json`, `seed-positions.sql`, and the presenter-only `v2-pages-77007-seeded-defect.json`. |
| `scripts/generate_fixtures.py` | Regenerates every fixture deterministically (`python3 scripts/generate_fixtures.py`). |

Code lives outside this directory: `position-service/src/main/java/.../api/v2/*` + `controller/PositionV2Controller.java`
(v2), `position-service/src/test/java/.../techfest/*` (contract + pagination tests), `database/initialSchema.sql`
(schema + seed), `web-front-end/react/src/report/*` (Portfolio report) and `web-front-end/react/src/App.tsx` (tab).

## Scenario refs

| Ref | Meaning |
|---|---|
| Setup branch | `devin/1790129721-techfest-migration` (PR against `main`) |
| Starting revision (scenario ref) | `3084e41ed92369fd2db1bc677bc3dab3d2b2c1d7` (first commit on the setup branch carrying the scenario; also recorded in the setup PR description) |
| Branch point | `7206ee800fb49cbf645ea97d8794009014c0d1ae` (`main` at the time of branching) |
| Remediation | a separate PR opened by the fresh session **against the setup branch**; never merged during the demo |

Select a ref: `git fetch origin && git checkout <ref>`, e.g. `git checkout 3084e41ed92369fd2db1bc677bc3dab3d2b2c1d7`
for the exact starting scenario (never `reset --hard` on shared branches).

## Demo run mode

The full docker-compose / Tilt stack is not required. Run exactly three processes; each in its own terminal from
the repo root. Requires Java 21 (`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`) and Node 20.

### Start

```bash
# 1. database (H2 TCP 18082). Seeds POSITIONS from database/initialSchema.sql on every start.
cd database && ./gradlew build -q && ./run.sh

# 2. position-service (HTTP 18090): serves v1 GET /positions/{accountId} and v2 GET /v2/positions
cd position-service && ./gradlew bootRun

# 3. React front-end (HTTP 18094): Blotter tab + Portfolio report tab
cd web-front-end/react && npm install && npm start
```

Open http://localhost:18094 → tab **Portfolio report** → account `77007` (default). The primary action is the
**Report source** toggle: `Current` vs `Legacy v1 (comparison)`.

### Seed

Seeding is automatic: `database/run.sh` runs `initialSchema.sql`, which contains the account 77007 rows
(generated copy in `fixtures/seed-positions.sql`). To re-seed from scratch, stop the database, delete
`database/_data/`, start it again. To regenerate fixtures after editing the generator:
`python3 position-service/techfest-migration/scripts/generate_fixtures.py`, then paste the new
`fixtures/seed-positions.sql` block into `database/initialSchema.sql`.

Smoke check the APIs:

```bash
curl -s localhost:18090/positions/77007 | jq length                      # 26
curl -s 'localhost:18090/v2/positions?accountId=77007' | jq '.items|length, .nextCursor'   # 10, "<cursor>"
```

### Run checks (default suites — must be green on the setup branch)

```bash
cd position-service && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test   # 13 tests: v1/v2 contract + pagination harness
cd web-front-end/react && CI=true npm test -- --watchAll=false                        # 12 tests: report consumer + fixture expectations
cd web-front-end/react && npm run build                                               # CRA build (pre-existing lint warnings in untouched files)
```

### Seeded-defect scenario (presenter only; expected RED)

Runs the fixture expectations against a recorded page set whose page 2 silently drops one row. It is **not**
part of the default suite and is never a statement about what the remediation session did.

```bash
cd web-front-end/react && CI=true TECHFEST_SCENARIO=seeded-defect npm test -- --watchAll=false src/report/summarize.test.ts
```

Expected: 2 failed (exact-once walk, per-currency summaries), 5 passed.

### Restore starting state

Touches only this scenario's data and refs.

```bash
# stop the three processes (Ctrl-C), then:
rm -rf database/_data                                   # scenario database files only (gitignored)
git checkout devin/1790129721-techfest-migration       # back to the setup branch
# remediation PR branches are left untouched; delete a local rehearsal branch only if you created it:
#   git branch -D <local-rehearsal-branch>
```

## Rehearsal record

Rehearsal 1 (2026-09-23, Normal mode, TASK_PROMPT.md verbatim, no steering):

| Item | Value |
|---|---|
| Starting revision | `3084e41ed92369fd2db1bc677bc3dab3d2b2c1d7` |
| Setup PR | https://github.com/COG-GTM/traderXCognitiondemos/pull/111 |
| Remediation session | https://app.devin.ai/sessions/701ee8f978794c40a714e773f534d309 (created 02:36 UTC, PR at 02:43 UTC) |
| Remediation PR | https://github.com/COG-GTM/traderXCognitiondemos/pull/112 against the setup branch, head `4439eed6fa859f44fc862618016b3aa1f96ba62f`, unmerged |
| Independent checks on `4439eed` (02:44-02:45 UTC) | React `CI=true npm test -- --watchAll=false` 19/19; `tsc --noEmit` clean; `./gradlew test` 13/13; live v2 walk 3 pages / 26 items / 26 unique / sorted; files changed limited to `web-front-end/react/src/**`; only v1 call left is `report/legacyPositions.ts` |
| Open question raised by the session | trade-feed socket payload still legacy-shaped (brief covers REST only) |
