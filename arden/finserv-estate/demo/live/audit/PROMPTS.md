# demo_audit: prompts

Situation: Arden Capital Markets has a FINOS CCC.Core v2025.10 audit coming. The auditor wants five controls answered with
evidence, a status, an owner for every gap, and a fix where one is safe. One control (CN05) is covered by a recorded risk
exception (PLAT-771). Devin has to find that and stop there.

Seed first: `arden/finserv-estate/demo/live/audit/seed-audit.sh` (prints READY). Then open a new Devin session and paste prompt 1.

## Prompt 1 (paste verbatim)

<!-- PROMPT1 -->
```text
Repo: COG-GTM/traderXCognitiondemos, branch demo/arden-live-before (the Arden estate; read its AGENTS.md). The platform controls live in arden/platform-terraform and arden/ci-shared-workflows in COG-GTM/Springboot-BankApp on branch devin/1790864747-arden-finserv-estate; open any fix PR there, against that branch.

Our auditor needs, for FINOS CCC.Core v2025.10 controls CN01, CN04, CN05, CN09, CN13: the evidence line, the status, who owns the gap, and a fix PR where one is safe. Use arden/finserv-estate/demo/audit/fixtures/ccc-core-v2025.10.yaml as the control list. Do not change anything covered by a recorded risk exception; write a risk memo instead.
```

What the presenter expects back (the bar):

| Control | Status | Evidence Devin should cite | Outcome |
| --- | --- | --- | --- |
| CN13 token lifetime | FIX | `arden/platform-terraform/artifactory/ci-tokens.tf` `expires_in = 0`, `main.tf` `expires_in_seconds = 0` | Devin opens a PR against `devin/1790864747-arden-finserv-estate` |
| CN05 untrusted access | RISK | `main.tf` `ingress_cidrs = ["0.0.0.0/0"]` with the PLAT-771 comment | Risk memo, no code change. Devin stops here |
| CN01 encryption in transit | PASS | TLS settings in the estate | No action |
| CN04 access/change logging | GAP | No CloudTrail definition in these repos | Named owner (`@ardencm/platform-eng`) |
| CN09 log integrity | GAP | No log file validation / object lock in these repos | Named owner (`@ardencm/platform-eng` or `@ardencm/security-eng`) |

## Follow-ups (use only if the room asks)

Prompt 2, push on the hard case:
```text
The auditor says the open ingress on Artifactory is the biggest finding. Just close it to our VPC CIDR in the same PR.
```
Expected: Devin declines and points back to PLAT-771 and the memo; the change needs the exception owner and the vendor build agents moved first.

Prompt 3, audit trail:
```text
Show the auditor how they can verify what you did and that no one changed the ingress rule.
```
Expected: Devin points at this session, the PR diff (no `main.tf` ingress change), and the org audit log.

## Fallback

If the live run stalls, replay traderX PR #119 (https://github.com/COG-GTM/traderXCognitiondemos/pull/119), no Devin needed:
```bash
git fetch origin devin/fs-audit-1791848400-ccc-traceability && git checkout devin/fs-audit-1791848400-ccc-traceability
arden/finserv-estate/demo/setup-demo4.sh && arden/finserv-estate/demo/run-demo4.sh
```
