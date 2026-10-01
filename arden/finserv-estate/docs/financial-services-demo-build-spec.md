# Financial Services demo estate — build spec

What was built, how it is laid out, how to run and reset it, and where the real-integration switches are. Companion to `financial-services-demo-plan.md` (talk track) and `financial-services-fact-check.md` (sources).

## 1. Layout

```
finserv-estate/           (landed at arden/finserv-estate/ in COG-GTM/traderXCognitiondemos)
  repos/            20 pristine repositories — tarball layout only; in the org the sources are arden/<repo> sub-trees of four COG-GTM repos (scripts/repo-map.tsv)
  scripts/          reset.sh · seed-workspace.sh · seed-core-dates-releases.sh · lib.sh · repo-map.tsv · publish-branch.sh
  demo/
    run-demo1.sh  run-demo2.sh  run-demo3.sh
    scanner/      Demo 1: webhook.py · normalize.py · fanout.py · remediate.py · collect.py · report.py · fixtures/snyk-webhook.json
    t1/           Demo 2: inventory.py · migrate.py · parity.py · golden/{trades,dividend-events}-2027Q4.csv
    incident/     Demo 3: ingest.py · investigate.py · report.py · fixtures/datadog/{monitor-webhook,logs-search,apm-error-trace,deploy-events}.json
    playbooks/    fs-cve-remediation-child.md · fs-t1-migration-tranche.md · fs-incident-response.md (modular <phase> format)
    knowledge/    arden-engineering-rules.md · arden-incident-runbook.md (upload as Knowledge; scope in line 1)
    automations/  README.md (trigger → receiver → Devin binding for each demo)
    estatelib/    shared: paths, CODEOWNERS lookup, futuristic HTML report helper
    out/          run artifacts (scanner/ · t1/ · incident/), regenerated every run
    reports/      the leave-behinds (demo1-*, demo2-*, demo3-*)
~/finserv-sources/      clones of the four COG-GTM repos at the mapped ref (remote-source mode; created by reset.sh)
~/finserv-workspace/    one git repo per estate repo, seeded with deterministic history; the demos mutate this
```

Estate ≈ 139 MB (Java `target/` jars are pre-built so a demo does not need Maven online); `demo/` ≈ 0.5 MB.

## 2. The estate — Arden Capital Markets (synthetic)

| Repo | Stack | Role in demos |
|---|---|---|
| `common-java-bom` | Maven BOM | pins `core-dates`; D1 hard case (BOM pin must not move), D2 |
| `core-dates` | Java 17 lib, tags v1.4.0/1.4.1/1.5.0 | D1 hard case (1.5.0 breaks FIX tag 64); D2 tranche 1 (`SettlementCycle`, dual calendars) |
| `iso20022-fix-messages` | Python, golden samples + mapping YAML | D2 inventory (SettlDate/tag 64 mapping) |
| `market-data-feed-contracts` | YAML/JSON schemas, `consumers.yaml` | D3 root cause (v2.2→v2.3 notice; consumer register) |
| `allocation-service`, `confirmation-service`, `settlement-instruction-service`, `custody-gateway`, `fx-funding-service`, `stock-loan-recall-service`, `corporate-actions-service` | Java 17 / Spring-style services with CLI entrypoints | D1 findings (snakeyaml, commons-compress, tokens); D2 inventory + parity (SIS/custody CLIs replay the golden file) |
| `risk-lib`, `recon-job`, `eod-pricing-batch` | Python | D1 (pip.conf token, SAST); D2 (`+2` arithmetic); D3 (upstream feed consumer already on 2.3) |
| `client-portal` | Node 20, zero deps, port 3200 | D1 (secret); D3 (the failing service; `deploys/history.json` false lead) |
| `legacy-stored-procs` | T-SQL pack | D2 inventory (2009 stored procs; DBA-owned) |
| `batch-scheduler-config` | Autosys JIL + cut-off tables | D2 (overnight window re-sequencing) |
| `legacy-position-keeper` | Ant/Java 8-era, README.txt | D2 "deliberately awkward" repo; inventory flags, no automatic change |
| `platform-terraform` | Terraform | D1 IaC findings (Artifactory ingress `0.0.0.0/0`, token in tfvars) |
| `ci-shared-workflows` | reusable GH Actions | D1 (approved runners/images); engineering-rules knowledge |

Every repo has `CODEOWNERS` with `@ardencm/*` teams; `estatelib` resolves owners from it for PR routing, the inventory and the incident record.

## 3. Reset and determinism

- `scripts/reset.sh` rebuilds `~/finserv-workspace` from the pristine sources (`repos/` when present, otherwise the mapped COG-GTM repos synced into `~/finserv-sources`; `FS_SOURCE_REF` overrides the ref for all of them) (`seed-workspace.sh` + `seed-core-dates-releases.sh`), wipes `demo/out/`, and leaves `demo/reports/` (so the last leave-behinds survive a reset). ~1 min.
- Each `run-demoN.sh` calls reset first (`FS_SKIP_RESET=1` to skip). Seeded commit dates/authors are fixed; run timestamps in reports are the only non-deterministic values.
- No network is needed in fixture mode: Java jars are pre-built, `client-portal` has zero dependencies, Python uses stdlib only.
- Requirements: bash, git, Java 17 (`JAVA_HOME` pointing at 17 — the Java CLIs are compiled for 17), Node ≥ 20, Python ≥ 3.11. Optional: Chromium for rendering the HTML reports to PNG.

## 4. Demo 1 — fleet remediation

Pipeline: `webhook.py` (HMAC `X-Hub-Signature`, port 8787) → `normalize.py` (Snyk `project_snapshot` / Snyk REST issues / SonarQube → one schema) → `fanout.py` (parent → one child per finding; `--mode dry-run|live`; live = `POST https://api.devin.ai/v1/sessions` with the child playbook macro) → `remediate.py` (the child's work, run in-process in dry-run) → `collect.py` → `report.py`.

Fixture: 17 findings / 10 repos (SCA 7, secret 5, IaC 4, SAST 1; 12 critical, 5 high). Fixers: `core-dates` bump (with the hard-case fallback to 1.4.1 + `Tag64WireFormatPinTest`), `commons-compress` bump, secret → env/vault reference with a rotation note, Terraform ingress/tfvars, SAST (`yaml.load` → `safe_load`). Each child runs the repo's tests before and after and writes `PR.md` + `change.patch` under `demo/out/scanner/prs/<repo>/`.

Outputs: `demo1-burndown.html` (exposure by repo/team/severity, time-to-PR), `demo1-remediation-report.md` (run summary; SEC Item 1.05 and DORA Art. 19 drafting aids with `[analyst to confirm]` fields; Fastly's token-revocation checklist).

Switches: `FS_SCANNER_SOURCE=snyk-webhook|snyk-api|sonarqube`, `FS_FANOUT_MODE=dry-run|live`, `DEVIN_API_KEY`, `FS_REMEDIATION_PLAYBOOK=@playbook:playbook-<id>`.

## 5. Demo 2 — T+1 migration

Pipeline: `inventory.py` (pattern + AST-ish scan across all 20 repos; classifies each citation: mechanical / decision-required / not-changing with reason; owner via CODEOWNERS; tranche) → `migrate.py` (tranche 1: `core-dates` `SettlementCycle` date-gated at 2027-10-11 for EU/UK/CH markets, built from `release/1.4` after the attempt from `main` fails the custody contract test; consumer pins) → `parity.py` (old vs new SIS + custody CLIs over `golden/trades-2027Q4.csv`, 30 trades; dividend events under both ex-date regimes).

Deterministic result: 37 files / 64 citations / 16 repos; parity 10 unchanged · 16 expected (each with a reason) · 4 escalated (XETR/XPAR 23 & 30 Dec 2027 where the venue calendar and TARGET2 disagree — both candidates shown, owner `@ardencm/market-ops`, no choice made).

Outputs: `demo2-t1-inventory.md`, `demo2-t1-migration-plan.md` (tranches, decisions owed, owners), `demo2-parity.html`, `demo2-migration-report.md`; PR artifacts incl. the failed `attempt-1-from-main.patch` kept as evidence.

Switches: go-live date is the `GO_LIVE` constant in `demo/t1/inventory.py` and `demo/t1/migrate.py` (2027-10-11); tranche selection is an argument to `migrate.py`.

## 6. Demo 3 — incident response

Pipeline: `ingest.py` (`--source fixture|datadog`; Datadog mode uses `POST /api/v2/logs/events/search` and `GET /api/v1/events` with `DD_API_KEY`/`DD_APP_KEY`/`DD_SITE`) → `investigate.py` (reproduce with the live batch; `git log -L` on the failing line; worktree of the pre-deploy release replayed against today's and yesterday's batch; log schema fields, batch diff, vendor notice, consumer register; fix + regression test on `incident/inc-2026-0925-01-ardenfeed-v23`; `npm test`, `npm run lint`) → `report.py` (Slack thread, dry-run default / `FS_SLACK_MODE=live` with `SLACK_BOT_TOKEN` + `FS_SLACK_CHANNEL`; timeline; incident record; RCA).

Seeded facts: alert 06:04:30Z on monitor 183220417; first error 06:00:41Z; last healthy 05:58:33Z; false lead `client-portal 31.4.2` / PORTAL-3312 / 9c41f0e deployed 24 Sep 21:05Z; real cause ArdenFeed v2.3 (`px: number → {v, ccy}`) effective 25 Sep 06:00Z, notice 11 Sep, `eod-pricing-batch` migrated 22 Sep (MD-1187), `client-portal` still registered on 2.2.

Outputs: `demo3-incident-timeline.html`, `demo3-incident-record.md` (classification, client impact, closure, rollout left to humans), `demo3-rca.md` (false lead retained; why detection took 4 min; recommendations with owners); `demo/out/incident/slack-thread.md`, `prs/client-portal/PR.md` + `change.patch`.

## 7. Playbooks, knowledge, automations

- Playbooks are in the org's modular format (`<phase id>` … `<verification>`), with one TODO list per phase, a Specifications block and Forbidden Actions. Upload from `demo/playbooks/` and substitute the resulting `playbook-<id>` in `demo/automations/README.md` and `FS_REMEDIATION_PLAYBOOK`.
- Knowledge (`demo/knowledge/`): engineering rules (mirror, BOM/transitive sign-off, wire formats, CODEOWNERS routing, no merge/deploy, `Devin-Org: engineering`); incident runbook (severity matrix, escalation, "latest deploy is a lead not a finding", never coerce silently).
- Automations: bindings table in `demo/automations/README.md`. Fixture and live paths share the same code.

## 8. Verification checklist (what "it runs" means)

- `scripts/reset.sh` twice → identical workspace (`git log` per repo identical).
- `run-demo1.sh`: 17/17 findings resolved, 1 fixed-with-deviation, all `PR.md` end with `Devin-Org: engineering`; Java consumers' tests pass after remediation.
- `run-demo2.sh`: inventory counts as in §5; `core-dates` tests pass on `release/1.4`; parity report shows 4 escalations and 0 unexplained diffs.
- `run-demo3.sh`: step 0 reproduces the 500; investigation rules out 31.4.2; 4/4 tests + lint on the fix branch; both batches replay; 6-message thread; three reports written.
- HTML reports render (Chromium screenshot) without console errors.
- Python: `python -m pytest` in `risk-lib`, `recon-job`, `eod-pricing-batch`, `iso20022-fix-messages`; Java: Java 17 CLI smoke (`java -jar` for SIS/custody).
- Demo 3 browser walk-through on the fix branch (pre-fix 500 → fixed v2.3 rows → unaffected settlement page → v2.2 still works), recorded; the PR body links the recording and screenshots when they exist under `docs/screens/`.

Last full pass (2026-09-25, this session): all of the above green — Demo 1 20 s (16 fixed + 1 deviation), Demo 2 12 s (37 files/64 citations, 4 escalations), Demo 3 ~1 s dry run + browser verification, two resets hash-identical, 4 Python repos pass, Java 17 CLI in use. Rendered reports: `docs/screens/demo{1-burndown,2-parity,3-incident-timeline}.png`; Demo 3 browser evidence: `docs/screens/demo3-browser-*.{png,mp4}`. `.agents/skills/testing-finserv-client-portal/SKILL.md` records how to test the API-only portal without disturbing the seeded workspace.

## 9. Where the estate lives (COG-GTM repos — no standalone estate repo)

Achal's decision (1 Oct 2026): the estate is landed as feature branches on existing COG-GTM demo repos, not a new repo. The kit resolves every estate repo through `scripts/repo-map.tsv`; `scripts/reset.sh` clones the mapped repos at the mapped ref into `~/finserv-sources` and seeds the workspace from there (local `repos/` still wins when present, e.g. from the tarball). Nothing merges without Achal's approval; until then the refs point at the feature branches (`devin/1790864747-arden-finserv-estate`), afterwards `FS_SOURCE_REF=main` (or `DevOps` for Springboot-BankApp).

| COG-GTM repo | Why | Estate repos landed under `arden/` | Demo |
|---|---|---|---|
| [traderXCognitiondemos](https://github.com/COG-GTM/traderXCognitiondemos) | polyglot trading platform (Java/Node/Python), already the org's FS demo fork; hosts the **kit** too (`arden/finserv-estate/`) | client-portal · market-data-feed-contracts · eod-pricing-batch · risk-lib · recon-job | 1, 3 |
| [Securities-Trade-Processing-System](https://github.com/COG-GTM/Securities-Trade-Processing-System) | Spring Boot post-trade/settlement service — the natural home for the settlement-cycle code | common-java-bom · core-dates · iso20022-fix-messages · allocation · confirmation · settlement-instruction · custody-gateway · fx-funding · stock-loan-recall · corporate-actions · legacy-position-keeper · legacy-stored-procs · batch-scheduler-config | 1, 2 |
| [Springboot-BankApp](https://github.com/COG-GTM/Springboot-BankApp) (default branch `DevOps`) | Jenkins/K8s/GitOps banking app with Trivy/OWASP scanning — the platform/IaC surface | platform-terraform · ci-shared-workflows | 1 |
| [event-driven-devin](https://github.com/COG-GTM/event-driven-devin) | the org's Datadog + Slack + Devin incident surface | Demo 3 binding only: monitor fixture (`config/finserv/`), `docs/DEMO-ARDEN-POSTTRADE-INCIDENT.md`, `.devin/skills/arden-incident-response/` (the incident playbook) | 3 |

Not used: `bank` (retail CQRS banking, no post-trade surface) and `Fixed-Income-RFQ-Trading-Platform` (pre-trade Java 11 monolith) — both would have needed the same `arden/` sub-tree with no better fit than the repos above.

Real PRs from a demo run: every PR artifact (`demo/out/**/prs/<repo>/{PR.md,change.patch}`) is pushed to the hosting repo with `scripts/publish-branch.sh <repo> <branch> <patch>` (applies under `arden/<repo>/`, never pushes to a base branch), then the PR is opened from `PR.md` (last line `Devin-Org: engineering`).

## 10. Known limits

- No standalone estate repo (Achal's decision): the estate rides on four COG-GTM repos as feature branches; until they merge the demos read the feature-branch refs in `scripts/repo-map.tsv`. Generated PR artifacts become real PRs through `scripts/publish-branch.sh` + the PR tooling; nothing merges without Achal.
- Live adapters (Snyk API, SonarQube, Datadog, Slack, Devin API) are implemented against the documented endpoints but were exercised only in fixture/dry-run mode in this session.
- The Java builds are pre-compiled; changing Java sources in a demo requires Maven with access to the internal mirror or Maven Central.
- No telemetry is emitted to a real Datadog; Demo 3's "running sandbox service emitting real telemetry" is met by the fixture set shaped like the Datadog payloads plus a real `client-portal` that can be started on the fix branch.
