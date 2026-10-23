# Financial Services: three-demo plan (runnable version)

Owner of the narrative: Achal Channarasappa (draft v1, 25 Sep 2026). This document turns that narrative into something an AE can run. It does not change the story; wording corrections from the fact-check are listed at the end and in `financial-services-fact-check.md`.

Estate: **Arden Capital Markets**, a synthetic post-trade estate of 20 repositories landed as `arden/<repo>` sub-trees on four existing COG-GTM repos (see *Where the estate lives*), one workspace the demos mutate (`~/finserv-workspace`), one reset (`scripts/reset.sh`, ~1 min). All three demos run deterministically from fixtures with no credentials; each has a real-adapter switch for the one integration that flips it from believable to personal.

| | Demo 1: fleet remediation | Demo 2: T+1 migration | Demo 3: incident response |
|---|---|---|---|
| Golden demo | security vulnerabilities | migrations | event-driven |
| Trigger | Snyk webhook (fixture or real) | programme milestone / manual | Datadog monitor (fixture or real) |
| Run | `demo/run-demo1.sh` (~40 s) | `demo/run-demo2.sh` (~3 min) | `demo/run-demo3.sh` (~1 min) |
| Playbook | `fs-cve-remediation-child` | `fs-t1-migration-tranche` | `fs-incident-response` |
| Leave-behind on screen | `demo1-burndown.html`, `demo1-remediation-report.md` | `demo2-parity.html`, `demo2-t1-migration-plan.md` | `demo3-incident-timeline.html`, `demo3-rca.md` |
| Hard case (scripted) | naive `core-dates` bump breaks FIX tag 64 → pin + contract test | venue vs TARGET2 calendar disagree → escalate, don't guess | latest deploy is innocent; feed changed shape |
| Result of the deterministic run | 17 findings → 16 fixes ready for review, 1 ready with deviation, 0 blocked | 37 files / 62 citations / 16 repos; 30 trades → 16 expected, 10 unchanged, 4 escalated | root cause found, deploy ruled out by replay, fix branch, record + RCA drafted |

---

## Demo 1: "Five days from disclosure to 400,000 exploitation attempts"

**Setup (before the call).** Run `demo/setup-demo.sh` on a fresh machine. For a presenter run, use `scripts/reset.sh` then `demo/run-demo1.sh`. Follow [`demo/TALK-TRACK.md`](../demo/TALK-TRACK.md) for measured timings and beats. Open `demo/reports/demo1-burndown.html` and `demo1-remediation-report.md` in tabs.

**Talk track → what's on screen**

1. Narrative (Artifactory CVE-2026-82329; five days; Wiz's three-CVE chain; Revolut/DriveWealth; Hilltop two weeks). No screen yet.
2. "Your scanner already told you." Show the Snyk-shaped webhook fixture: 17 findings, 10 repos, SCA + secrets + IaC + SAST. Point at the Artifactory tokens committed in five repos: that is the CVE's blast radius, not the CVE itself.
3. Run `demo/run-demo1.sh`. Narrate the log: webhook signature checked → normalised → **one child session per finding** (dry-run here; `FS_FANOUT_MODE=live` makes real sessions) → remediation → report. Under a minute.
4. Burn-down: review-ready fixes by repo, team, severity and time-to-PR. The unaddressed count is computed from the run; no PR is described as merged.
5. **Hard case.** Open the top panel and `settlement-instruction-service/PR.md`: Devin tested `core-dates 1.5.0`, the custody-gateway contract test failed because tag 64 became an ISO date, then Devin read the changelog, pinned 1.4.1 and added `Tag64WireFormatPinTest`. Both attempts and the CODEOWNERS reviewer are shown. "Devin did not follow the scanner blindly; Devin followed the evidence."
6. Remediation report: SEC Item 1.05 and DORA Article 19 drafting aids, populated from the run, with every judgment field marked `[analyst to confirm]`. "Generated, not assembled. Devin does not make the materiality call."
7. Close: nothing merged. Every change is a PR to its CODEOWNERS team. Devin is the remediation layer; the scanner, the SOC and branch protection stay where they are.

**Progressive prompts (Ask Devin → Ask Devin → session)**
- *Ask:* "Read this Snyk export for the Arden estate and tell me which findings share a root cause and which repos own them." 
- *Ask:* "For the `core-dates` findings, what would a straight version bump break downstream, and what would you do instead?"
- *Session:* "Remediate every finding in this export as one child session per finding using `@playbook:playbook-<fs-cve-remediation-child id>`, and give me the burn-down and the SEC/DORA drafting aid." 

**Customise.** Swap the fixture for their scanner (`FS_SCANNER_SOURCE=snyk-api|sonarqube`, adapters in `demo/scanner/normalize.py`); swap the CVE in `demo/scanner/fixtures/snyk-webhook.json`; for a CISO lead with §4–6, for platform engineering lead with §3 and `demo/knowledge/arden-engineering-rules.md`.

---

## Demo 2: "T+1 lands on 11 October 2027"

**Setup.** Run `scripts/reset.sh` then `demo/run-demo2.sh`. Open `demo2-t1-inventory.md`, `demo2-t1-migration-plan.md` and `demo2-parity.html`. Follow [`demo/TALK-TRACK.md`](../demo/TALK-TRACK.md) for measured timings.

**Talk track → what's on screen**

1. Narrative (just over twelve months; the joint testing plan's three messages; "~20% of processing time"; T+1 is the removal of the overnight window). 
2. "Every firm is running a discovery programme. Archaeology." Run the inventory step. The report shows 37 files, 62 citations and 16 of 20 repositories, with file/line evidence, CODEOWNERS owner, tranche and rationale.
3. Migration plan: three tranches, with decisions owed to named business owners (calendar authority, ex-dates, dual listings, FX spot and recall notice periods). "Devin records the questions that require a human decision."
4. Execute tranche 1. Show the migration report and test evidence; no branches are merged and no business decision is made by the migration.
5. Parity: one golden file of 30 Q4-2027 trades through the old and new CLIs. Show the expected and unchanged outcomes, then the four calendar escalations. The venue-calendar and TARGET2 candidate dates, owner and "no choice made" status are explicit. Dividend events are shown under both ex-date regimes.
6. Close on `demo2-migration-report.md`: what changed, what is on branches and what humans owe. "Mechanical changes are separated from the questions that need business authority."

**Progressive prompts**
- *Ask:* "Inventory every settlement-cycle assumption in the Arden estate for the EU/UK/CH T+1 move on 11 Oct 2027, with file/line citations and CODEOWNERS." 
- *Ask:* "Which of those are mechanical, which need a business decision, and who owns each decision?"
- *Session:* "Execute tranche 1 with `@playbook:playbook-<fs-t1-migration-tranche id>` and replay `demo/t1/golden/trades-2027Q4.csv` through old and new; escalate anything the calendars disagree on."

**Customise.** Swap the driver (ISO 20022 completion, EMIR Refit, CFPB 1033) by re-pointing `demo/t1/inventory.py`'s patterns and classification table; the parity harness is driver-agnostic (any CLI that maps input rows to output rows). If they have a T+1 scope spreadsheet, run the inventory against it and diff.

---

## Demo 3: "06:04, before the London open"

**Setup.** Run `scripts/reset.sh` then `demo/run-demo3.sh`. Follow [`demo/TALK-TRACK.md`](../demo/TALK-TRACK.md). Open the client portal on main and the incident fix branch, then open `demo3-incident-timeline.html`, `demo3-incident-record.md` and `demo3-rca.md`.

**Talk track → what's on screen**

1. Narrative (Hilltop two weeks; TruStage ten weeks; the small monthly version: an on-call engineer at dawn with six dashboards and forty changes; DORA makes the record a supervisory artefact).
2. Step 0 leads with the Datadog alert: `client-portal / valuation 5xx rate`, 100%, and the last deploy in the alert body, **31.4.2, "memoise valuation rows", 21:05 last night**. The client portal shows the 500 state for the live batch.
3. Watch the investigation log. Step 1 reproduces with the live batch. Step 2 tests the deploy: `git log -L` on the failing line, checks out the pre-deploy release into a worktree, replays the same batch: **fails identically**; replays yesterday's batch: works; seven healthy requests on the new version overnight. **Ruled out. A rollback would have kept the portal down.**
4. Step 3: last healthy request carried feed schema 2.2, first error carries 2.3. Batch diff: `px 12.34 → {"v": 12.41, "ccy": "GBP"}`. Vendor notice from 11 Sep: effective 25 Sep 06:00, no parallel run. Consumer register: `eod-pricing-batch` moved to 2.3 on 22 Sep, `client-portal` left on 2.2: "MD-1187 scope was EOD only". Root cause, three independent pieces of evidence.
5. Step 4: fix on a branch: normaliser accepts both shapes and *throws* on a third rather than coercing; regression test with the live batch; 4/4 tests, lint clean, both batches replay. Not deployed.
6. The timeline, incident record and RCA preserve the false lead and evidence. "Devin tested the first explanation and disproved it with the pre-deploy replay."

**Progressive prompts**
- *Ask:* "This Datadog alert just fired on client-portal: what does the stack trace point at, and what changed in the last 24 hours?"
- *Ask:* "Before we roll back 31.4.2: replay the live quote batch on the previous release and tell me whether the deploy is actually the cause."
- *Session:* "Run the incident with `@playbook:playbook-<fs-incident-response id>`: root-cause it, fix on a branch with a regression test, and draft the incident record and RCA for ops risk."

**Customise.** Point `--source datadog` at their monitor (needs `DD_API_KEY`/`DD_APP_KEY`); their post-mortem becomes the fixture by editing `demo/incident/fixtures/datadog/*.json` and the seeded repo history in `scripts/seed-workspace.sh`. Trading → this scenario; payments → swap the feed for a rail cut-off file; retail → the portal itself.

---

## Wording corrections applied to the narrative (from the fact-check)

- Demo 1 title: "Four days … 400,000 attacks" → "Five days … 400,000 exploitation attempts". Fastly: ~98% of 31 Aug volume was researchers; lean on 1–2 Sep.
- Added: Wiz (14 Sep): 82329 is the third Artifactory auth CVE since July, chained with 42016/42018 to drop backdoors.
- "ESAs' Joint Examination Teams ask for evidence" → JETs examine critical ICT *providers*; a bank's evidence requests come via its NCA. DORA clock stated exactly: 4h from classification / 24h from awareness.
- "FFIEC 36-hour rule" → OCC/Fed/FDIC Computer-Security Incident Notification rule.
- Hilltop "over a week" → two weeks (restored 22 Sep). TruStage "eight weeks" → ten. T+1 "thirteen months" → just over twelve.
- Revolut sources → Reuters / The Register / Finance Magnates; DriveWealth still clears Revolut's US business (second incident 24–25 Sep).
- "60–70% of engineering budget is run-the-bank": **unsourced**; present as "the figure we hear in discovery" or use the prospect's number.

## What is deliberately not in the package

- No Insurance build. No merges, no deployments, no live regulatory classification. No new GitHub repo: per Achal, the estate rides on existing COG-GTM repos as feature branches (mapping below); nothing merges without his approval.

## Where the estate lives (COG-GTM repos: no standalone estate repo)

Achal's decision (1 Oct 2026): the estate is landed as feature branches on existing COG-GTM demo repos, not a new repo. The kit resolves every estate repo through `scripts/repo-map.tsv`; `scripts/reset.sh` clones the mapped repos at the mapped ref into `~/finserv-sources` and seeds the workspace from there (local `repos/` still wins when present, e.g. from the tarball). Nothing merges without Achal's approval; until then the refs point at the feature branches (`devin/1790864747-arden-finserv-estate`), afterwards `FS_SOURCE_REF=main` (or `DevOps` for Springboot-BankApp).

| COG-GTM repo | Why | Estate repos landed under `arden/` | Demo |
|---|---|---|---|
| [traderXCognitiondemos](https://github.com/COG-GTM/traderXCognitiondemos) | polyglot trading platform (Java/Node/Python), already the org's FS demo fork; hosts the **kit** too (`arden/finserv-estate/`) | client-portal · market-data-feed-contracts · eod-pricing-batch · risk-lib · recon-job | 1, 3 |
| [Securities-Trade-Processing-System](https://github.com/COG-GTM/Securities-Trade-Processing-System) | Spring Boot post-trade/settlement service: the natural home for the settlement-cycle code | common-java-bom · core-dates · iso20022-fix-messages · allocation · confirmation · settlement-instruction · custody-gateway · fx-funding · stock-loan-recall · corporate-actions · legacy-position-keeper · legacy-stored-procs · batch-scheduler-config | 1, 2 |
| [Springboot-BankApp](https://github.com/COG-GTM/Springboot-BankApp) (default branch `DevOps`) | Jenkins/K8s/GitOps banking app with Trivy/OWASP scanning: the platform/IaC surface | platform-terraform · ci-shared-workflows | 1 |
| [event-driven-devin](https://github.com/COG-GTM/event-driven-devin) | the org's Datadog + Slack + Devin incident surface | Demo 3 binding only: monitor fixture (`config/finserv/`), `docs/DEMO-ARDEN-POSTTRADE-INCIDENT.md`, `.devin/skills/arden-incident-response/` (the incident playbook) | 3 |

Not used: `bank` (retail CQRS banking, no post-trade surface) and `Fixed-Income-RFQ-Trading-Platform` (pre-trade Java 11 monolith): both would have needed the same `arden/` sub-tree with no better fit than the repos above.

Real PRs from a demo run: every PR artifact (`demo/out/**/prs/<repo>/{PR.md,change.patch}`) is pushed to the hosting repo with `scripts/publish-branch.sh <repo> <branch> <patch>` (applies under `arden/<repo>/`, never pushes to a base branch), then the PR is opened from `PR.md` (last line `Devin-Org: engineering`).
