# Financial Services — three-demo plan (runnable version)

Owner of the narrative: Achal Channarasappa (draft v1, 25 Sep 2026). This document turns that narrative into something an AE can run. It does not change the story; wording corrections from the fact-check are listed at the end and in `financial-services-fact-check.md`.

Estate: **Arden Capital Markets**, a synthetic post-trade estate of 20 repositories landed as `arden/<repo>` sub-trees on four existing COG-GTM repos (see *Where the estate lives*), one workspace the demos mutate (`~/finserv-workspace`), one reset (`scripts/reset.sh`, ~1 min). All three demos run deterministically from fixtures with no credentials; each has a real-adapter switch for the one integration that flips it from believable to personal.

| | Demo 1 — fleet remediation | Demo 2 — T+1 migration | Demo 3 — incident response |
|---|---|---|---|
| Golden demo | security vulnerabilities | migrations | event-driven |
| Trigger | Snyk webhook (fixture or real) | programme milestone / manual | Datadog monitor (fixture or real) |
| Run | `demo/run-demo1.sh` (~40 s) | `demo/run-demo2.sh` (~3 min) | `demo/run-demo3.sh` (~1 min) |
| Playbook | `fs-cve-remediation-child` | `fs-t1-migration-tranche` | `fs-incident-response` |
| Leave-behind on screen | `demo1-burndown.html`, `demo1-remediation-report.md` | `demo2-parity.html`, `demo2-t1-migration-plan.md` | `demo3-incident-timeline.html`, `demo3-rca.md` |
| Hard case (scripted) | naive `core-dates` bump breaks FIX tag 64 → pin + contract test | venue vs TARGET2 calendar disagree → escalate, don't guess | latest deploy is innocent; feed changed shape |
| Result of the deterministic run | 17 findings → 16 fixed, 1 fixed-with-deviation, 0 blocked | 37 files / 64 citations / 16 repos; 30 trades → 16 expected, 10 unchanged, 4 escalated | root cause found, deploy ruled out by replay, fix branch, record + RCA drafted |

---

## Demo 1 — "Five days from disclosure to 400,000 exploitation attempts"

**Setup (before the call).** `demo/run-demo1.sh` after `scripts/reset.sh`. Open `demo/reports/demo1-burndown.html` and `demo1-remediation-report.md` in tabs; have `demo/out/scanner/prs/settlement-instruction-service/PR.md` ready — that is the hard case.

**Talk track → what's on screen**

1. Narrative (Artifactory CVE-2026-82329; five days; Wiz's three-CVE chain; Revolut/DriveWealth; Hilltop two weeks). No screen yet.
2. "Your scanner already told you." Show the Snyk-shaped webhook fixture: 17 findings, 10 repos, SCA + secrets + IaC + SAST. Point at the Artifactory tokens committed in five repos — that is the CVE's blast radius, not the CVE itself.
3. Run `demo/run-demo1.sh`. Narrate the log: webhook signature checked → normalised → **one child session per finding** (dry-run here; `FS_FANOUT_MODE=live` makes real sessions) → remediation → report. Under a minute.
4. Burn-down: exposure by repo, team, severity, time-to-PR. Fixed-with-deviation is a different colour on purpose.
5. **Hard case.** `settlement-instruction-service/PR.md`: scanner said `core-dates 1.5.0`; the child tried it, the custody-gateway contract test failed (tag 64 became ISO dates), it read the changelog, pinned 1.4.1 (security fix, wire format intact) and added `Tag64WireFormatPinTest`. Both attempts are in the PR. "It didn't do what the scanner said; it did what the scanner meant."
6. Remediation report: SEC Item 1.05 and DORA Art. 19 drafting aids, populated from the run, with every judgment field marked `[analyst to confirm]`. "Generated, not assembled — and it doesn't pretend to make the materiality call."
7. Close: nothing merged. Every change is a PR to its CODEOWNERS team. Devin is the remediation layer; the scanner, the SOC and branch protection stay where they are.

**Progressive prompts (Ask Devin → Ask Devin → session)**
- *Ask:* "Read this Snyk export for the Arden estate and tell me which findings share a root cause and which repos own them." 
- *Ask:* "For the `core-dates` findings, what would a straight version bump break downstream, and what would you do instead?"
- *Session:* "Remediate every finding in this export as one child session per finding using `@playbook:playbook-<fs-cve-remediation-child id>`, and give me the burn-down and the SEC/DORA drafting aid." 

**Customise.** Swap the fixture for their scanner (`FS_SCANNER_SOURCE=snyk-api|sonarqube`, adapters in `demo/scanner/normalize.py`); swap the CVE in `demo/scanner/fixtures/snyk-webhook.json`; for a CISO lead with §4–6, for platform engineering lead with §3 and `demo/knowledge/arden-engineering-rules.md`.

---

## Demo 2 — "T+1 lands on 11 October 2027"

**Setup.** `demo/run-demo2.sh` (includes the reset). Open `demo2-t1-inventory.md`, `demo2-t1-migration-plan.md`, `demo2-parity.html`. Have `demo/out/t1/prs/core-dates/PR.md` and `attempt-1-from-main.patch` ready.

**Talk track → what's on screen**

1. Narrative (just over twelve months; the joint testing plan's three messages; "~20% of processing time"; T+1 is the removal of the overnight window). 
2. "Every firm is running a discovery programme. Archaeology." Run the inventory step. 37 files, 64 citations, 16 of 20 repos — including the stored procedure from 2009 and the scheduler config nobody owns — each with file/line, snippet, CODEOWNERS owner, tranche, or an explicit reason it is *not* changing.
3. Migration plan: three tranches, the decisions owed to named business owners (calendar authority, ex-dates, dual listings, FX spot, recall notice periods). "It knows what it is not allowed to decide."
4. Execute tranche 1. **Hard case A**: the date-gated change built from `main` fails the custody contract test (main had already regressed tag 64 to ISO dates in 1.5.0); Devin keeps that patch as evidence, rebuilds from `release/1.4`, ships `core-dates 1.4.2`, and the BOM pin does not move. 
5. Parity: one golden file of 30 Q4-2027 trades through the old and new CLIs. 10 unchanged (pre-go-live, North America), 16 expected changes (each explained), **4 escalated**: XETR/XPAR trades on 23 and 30 Dec where the venue calendar and TARGET2 disagree on the settlement date. Both candidates shown; owner named; nothing chosen. Two dividend events straddling the cutover shown under both ex-date regimes. **Hard case B.**
6. Close on `demo2-migration-report.md`: what changed, what is on branches, what humans owe. "Six months of discovery is now a document you can argue with."

**Progressive prompts**
- *Ask:* "Inventory every settlement-cycle assumption in the Arden estate for the EU/UK/CH T+1 move on 11 Oct 2027, with file/line citations and CODEOWNERS." 
- *Ask:* "Which of those are mechanical, which need a business decision, and who owns each decision?"
- *Session:* "Execute tranche 1 with `@playbook:playbook-<fs-t1-migration-tranche id>` and replay `demo/t1/golden/trades-2027Q4.csv` through old and new; escalate anything the calendars disagree on."

**Customise.** Swap the driver (ISO 20022 completion, EMIR Refit, CFPB 1033) by re-pointing `demo/t1/inventory.py`'s patterns and classification table; the parity harness is driver-agnostic (any CLI that maps input rows to output rows). If they have a T+1 scope spreadsheet, run the inventory against it and diff.

---

## Demo 3 — "06:04, before the London open"

**Setup.** `demo/run-demo3.sh` (includes the reset). Open `demo/out/incident/slack-thread.md` (or a real channel with `FS_SLACK_MODE=live`), `demo3-incident-timeline.html`, `demo3-incident-record.md`, `demo3-rca.md`.

**Talk track → what's on screen**

1. Narrative (Hilltop two weeks; TruStage ten weeks; the small monthly version: an on-call engineer at dawn with six dashboards and forty changes; DORA makes the record a supervisory artefact).
2. Step 0 of the run shows what the portal sees: `GET /api/portfolio/valuation → 500 TypeError: n.px.toFixed is not a function`. Then the Datadog alert (fixture shaped like the real webhook): `client-portal / valuation 5xx rate`, 100%, with the last deploy helpfully quoted in the alert body — **31.4.2, "memoise valuation rows", 21:05 last night**. Everyone in the room now believes the deploy did it.
3. Watch the investigation log. Step 1 reproduces with the live batch. Step 2 tests the deploy: `git log -L` on the failing line, checks out the pre-deploy release into a worktree, replays the same batch — **fails identically**; replays yesterday's batch — works; seven healthy requests on the new version overnight. **Ruled out. A rollback would have kept the portal down.**
4. Step 3: last healthy request carried feed schema 2.2, first error carries 2.3. Batch diff: `px 12.34 → {"v": 12.41, "ccy": "GBP"}`. Vendor notice from 11 Sep: effective 25 Sep 06:00, no parallel run. Consumer register: `eod-pricing-batch` moved to 2.3 on 22 Sep, `client-portal` left on 2.2 — "MD-1187 scope was EOD only". Root cause, three independent pieces of evidence.
5. Step 4: fix on a branch — normaliser accepts both shapes and *throws* on a third rather than coercing; regression test with the live batch; 4/4 tests, lint clean, both batches replay. Not deployed.
6. The thread, the timeline, the incident record with classification left to ops risk, the RCA with the wrong lead kept in. "The human's job became approving a judgement. And you watched it disprove itself."

**Progressive prompts**
- *Ask:* "This Datadog alert just fired on client-portal — what does the stack trace point at, and what changed in the last 24 hours?"
- *Ask:* "Before we roll back 31.4.2: replay the live quote batch on the previous release and tell me whether the deploy is actually the cause."
- *Session:* "Run the incident with `@playbook:playbook-<fs-incident-response id>`: root-cause it, fix on a branch with a regression test, and draft the incident record and RCA for ops risk."

**Customise.** Point `--source datadog` at their monitor (needs `DD_API_KEY`/`DD_APP_KEY`); their post-mortem becomes the fixture by editing `demo/incident/fixtures/datadog/*.json` and the seeded repo history in `scripts/seed-workspace.sh`. Trading → this scenario; payments → swap the feed for a rail cut-off file; retail → the portal itself.

---

## Wording corrections applied to the narrative (from the fact-check)

- Demo 1 title: "Four days … 400,000 attacks" → "Five days … 400,000 exploitation attempts". Fastly: ~98% of 31 Aug volume was researchers; lean on 1–2 Sep.
- Added: Wiz (14 Sep) — 82329 is the third Artifactory auth CVE since July, chained with 42016/42018 to drop backdoors.
- "ESAs' Joint Examination Teams ask for evidence" → JETs examine critical ICT *providers*; a bank's evidence requests come via its NCA. DORA clock stated exactly: 4h from classification / 24h from awareness.
- "FFIEC 36-hour rule" → OCC/Fed/FDIC Computer-Security Incident Notification rule.
- Hilltop "over a week" → two weeks (restored 22 Sep). TruStage "eight weeks" → ten. T+1 "thirteen months" → just over twelve.
- Revolut sources → Reuters / The Register / Finance Magnates; DriveWealth still clears Revolut's US business (second incident 24–25 Sep).
- "60–70% of engineering budget is run-the-bank" — **unsourced**; present as "the figure we hear in discovery" or use the prospect's number.

## What is deliberately not in the package

- No Insurance build. No merges, no deployments, no live regulatory classification. No new GitHub repo: per Achal, the estate rides on existing COG-GTM repos as feature branches (mapping below); nothing merges without his approval.

## Where the estate lives (COG-GTM repos — no standalone estate repo)

Achal's decision (1 Oct 2026): the estate is landed as feature branches on existing COG-GTM demo repos, not a new repo. The kit resolves every estate repo through `scripts/repo-map.tsv`; `scripts/reset.sh` clones the mapped repos at the mapped ref into `~/finserv-sources` and seeds the workspace from there (local `repos/` still wins when present, e.g. from the tarball). Nothing merges without Achal's approval; until then the refs point at the feature branches (`devin/1790864747-arden-finserv-estate`), afterwards `FS_SOURCE_REF=main` (or `DevOps` for Springboot-BankApp).

| COG-GTM repo | Why | Estate repos landed under `arden/` | Demo |
|---|---|---|---|
| [traderXCognitiondemos](https://github.com/COG-GTM/traderXCognitiondemos) | polyglot trading platform (Java/Node/Python), already the org's FS demo fork; hosts the **kit** too (`arden/finserv-estate/`) | client-portal · market-data-feed-contracts · eod-pricing-batch · risk-lib · recon-job | 1, 3 |
| [Securities-Trade-Processing-System](https://github.com/COG-GTM/Securities-Trade-Processing-System) | Spring Boot post-trade/settlement service — the natural home for the settlement-cycle code | common-java-bom · core-dates · iso20022-fix-messages · allocation · confirmation · settlement-instruction · custody-gateway · fx-funding · stock-loan-recall · corporate-actions · legacy-position-keeper · legacy-stored-procs · batch-scheduler-config | 1, 2 |
| [Springboot-BankApp](https://github.com/COG-GTM/Springboot-BankApp) (default branch `DevOps`) | Jenkins/K8s/GitOps banking app with Trivy/OWASP scanning — the platform/IaC surface | platform-terraform · ci-shared-workflows | 1 |
| [event-driven-devin](https://github.com/COG-GTM/event-driven-devin) | the org's Datadog + Slack + Devin incident surface | Demo 3 binding only: monitor fixture (`config/finserv/`), `docs/DEMO-ARDEN-POSTTRADE-INCIDENT.md`, `.devin/skills/arden-incident-response/` (the incident playbook) | 3 |

Not used: `bank` (retail CQRS banking, no post-trade surface) and `Fixed-Income-RFQ-Trading-Platform` (pre-trade Java 11 monolith) — both would have needed the same `arden/` sub-tree with no better fit than the repos above.

Real PRs from a demo run: every PR artifact (`demo/out/**/prs/<repo>/{PR.md,change.patch}`) is pushed to the hosting repo with `scripts/publish-branch.sh <repo> <branch> <patch>` (applies under `arden/<repo>/`, never pushes to a base branch), then the PR is opened from `PR.md` (last line `Devin-Org: engineering`).
