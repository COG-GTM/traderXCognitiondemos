# Financial Services — discovery cheat sheet (one page)

Use before choosing which of the three demos to lead with, and to pick the customisation levers. Question style follows the org's discovery bank (before-scenario → negative consequence → qualifying).

## Eight questions

| # | Ask | Why it matters / what it decides |
|---|---|---|
| 1 | **"What was your last fire drill — Log4Shell, MOVEit, Citrix Bleed, the Artifactory bypass? How many repos, how many teams, how long from advisory to last PR merged?"** | Picks the CVE for Demo 1 and gives you their own before-scenario numbers. If they answer in days → Demo 1 is the lead. |
| 2 | **"Which scanner is the source of truth — Snyk, Wiz, SonarQube, Dependabot — and who owns the findings once they're in it? How many are open today?"** | Decides fixture vs. real adapter (`FS_SCANNER_SOURCE`). "Security owns the list, app teams own the fix" is the gap Demo 1 shows. No repo access for Devin → red flag. |
| 3 | **"What mandated change is on the 18-month roadmap — T+1 (EU/UK/CH, 11 Oct 2027), ISO 20022 completion, EMIR Refit, CFPB 1033, Basel/FRTB? Is there a discovery programme running for it, and what does it cost per month?"** | Picks the Demo 2 driver. Analyst-months on discovery is the negative-consequence number. If they have a scope spreadsheet, offer to run the inventory against it. |
| 4 | **"How many places in your code do you think assume a settlement cycle, a batch window or a cut-off? Who would know?"** | If the answer is "nobody knows" → Demo 2 lead. Also surfaces the legacy pockets (stored procs, scheduler config, an Ant build) that decide how awkward the sandbox should look. |
| 5 | **"When a P2 fires at 06:00, who joins, what do they open, and how long until someone can say what changed? What's your mean time to *cause*, not to restore?"** | Demo 3 before-scenario. Toolchain answer (Datadog / Splunk / Dynatrace; Slack / Teams; Jira / ServiceNow) sets the adapter swap. |
| 6 | **"Who writes the incident record and the RCA today, and has a regulator or auditor asked to see one in the last year?"** (EU: DORA Art. 17–20 · US: SEC Item 1.05, OCC/Fed/FDIC 36-hour rule · UK: PRA/FCA impact tolerances · NY: Part 500) | Decides whether the evidence pack or the fix PR is the hero in Demo 3, and which drafting aid to show in Demo 1's report. Never present Devin as making the classification. |
| 7 | **"Where does Devin's output have to land — PRs to CODEOWNERS with branch protection, a change ticket, both? What can never be automated?"** | Confirms the no-merge/no-deploy posture the estate assumes; surfaces knowledge entries to write (approved mirrors, transitive-bump sign-off, base images). |
| 8 | **"If we could show one of these against your own repos rather than a sandbox — which one, and who would need to be in the room?"** | Sizes the pilot and names the economic buyer (CISO for 1, Head of Post-Trade/programme for 2, Head of SRE/Operational Resilience for 3). |

## Levers (what to change in the estate for a given prospect)

| Lever | Where | Effort |
|---|---|---|
| CVE / finding mix | `demo/scanner/fixtures/snyk-webhook.json` | minutes |
| Scanner format | `FS_SCANNER_SOURCE=snyk-webhook\|snyk-api\|sonarqube`; add adapter in `demo/scanner/normalize.py` | hours for a new tool |
| Live fan-out to real child sessions | `FS_FANOUT_MODE=live` + `DEVIN_API_KEY` + uploaded playbook id | minutes |
| Regulatory drafting aid shown | `demo/scanner/report.py` (SEC 1.05 / DORA 19 today; add OCC 36h, PRA) | an hour |
| Migration driver | `demo/t1/inventory.py` patterns + classification table; golden file | a session |
| Go-live date / tranche | `GO_LIVE` constant in `demo/t1/inventory.py` + `migrate.py`; tranche argument to `migrate.py` | minutes |
| Their scope spreadsheet | export to CSV, diff against `demo/out/t1/inventory.json` | an hour |
| Incident scenario | `demo/incident/fixtures/datadog/*.json` + seeded history in `scripts/seed-workspace.sh` | a session (their post-mortem → fixture) |
| APM / chat | `--source datadog` (`DD_API_KEY`, `DD_APP_KEY`); `FS_SLACK_MODE=live` | minutes if creds exist |
| House rules | `demo/knowledge/*.md` → Knowledge entries | minutes |
| Audience tilt | CISO/resilience: lead with reports, demote PRs. Platform eng: lead with fan-out + knowledge rules. CFO-adjacent: analyst-months and fail/penalty exposure. | talk-track only |

## Peer incidents by sub-segment (verified 25 Sep 2026; re-check dates before a call)

- Payments/fintech → Revolut (two Sep 2026 incidents; DriveWealth third-party, still clears Revolut's US business).
- Community/regional banks → Hilltop National Bank (offline 8–22 Sep 2026; $1,000/day card cap).
- Clearing/post-trade → ICBC Financial Services (LockBit, Nov 2023; Treasury settlement).
- Any Artifactory shop → CVE-2026-82329 + the 42016/42018 chain (Fastly 3 Sep, Wiz 14 Sep 2026).

## Disqualifiers to listen for
- Devin cannot reach the repos or the build (air-gapped, no cloud egress) → Windsurf/CLI conversation instead.
- The change needs a system Devin cannot touch to verify (mainframe-only test env, vendor UAT) → scope to inventory/plan (Demo 2 first half) only.
- "We want it to merge and deploy on its own" → reset expectations; every demo here ends at a PR and a human approval.

Unsourced figure to avoid stating as fact: "60–70% of engineering spend is run-the-bank" — ask for theirs instead.
