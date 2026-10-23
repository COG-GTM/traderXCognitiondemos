# demo_fleetwide: prompts

Paste these into a **new Devin session** (web app, default agent). The session needs access to
`COG-GTM/traderXCognitiondemos`, `COG-GTM/Securities-Trade-Processing-System` and `COG-GTM/Springboot-BankApp`
(the org default). Run `setup-fleetwide.sh` first; it reseeds the situation and prints prompt 1.

Child playbook: `playbook-1db82f8448944ee7b94282f275798b59` (FS CVE remediation: child session (one finding)).

## Prompt 1: stage run (5 findings, the default)

```text
A Snyk snapshot for incident SEC-2026-0912 (CVE-2026-82329, the Artifactory authentication bypass) reports 17 findings across the Arden estate. For this run, remediate only these 5:
- settlement-instruction-service SNYK-JAVA-ORGYAML-3152153
- custody-gateway SNYK-JAVA-ORGAPACHECOMMONS-6254296
- fx-funding-service SNYK-SECRET-ARDEN-0418
- platform-terraform SNYK-CC-ARDEN-TOKENTTL
- risk-lib SNYK-CODE-PY-YAMLLOAD-0091

Work from COG-GTM/traderXCognitiondemos branch demo/arden-live-before and read AGENTS.md first. Normalise arden/finserv-estate/demo/scanner/fixtures/snyk-webhook.json with demo/scanner/normalize.py, keep the 5 findings above, and generate one child prompt per finding with demo/scanner/fanout.py in dry-run mode with FS_REMEDIATION_PLAYBOOK=@playbook:playbook-1db82f8448944ee7b94282f275798b59.
Then create one child Devin session per finding with playbook_id playbook-1db82f8448944ee7b94282f275798b59, using its generated prompt plus these rules: open the PR on the COG-GTM repo that hosts that estate repo (scripts/repo-map.tsv) against its mapped branch, on a branch named security/<repo>-<scanner id lowercased>, with no em dashes in the PR title or body. One finding per child, do not fix anything yourself, never merge.
Show me the fan-out plan while the children work. When every child has reported its STATUS line, give me a burn-down table (finding, repo, status, PR, one-line why) and call out any child that did not take the scanner's suggested upgrade, and why.
```

What the presenter says while Devin works: see `TALK-TRACK.md`.

## Prompt 1 (full): all 17 findings

Same as prompt 1, with the first paragraph replaced by:

```text
A Snyk snapshot for incident SEC-2026-0912 (CVE-2026-82329, the Artifactory authentication bypass) reports 17 findings across the Arden estate. Remediate all 17.
```

and "keep the 5 findings above" removed. Expect 17 children; only run this when the meeting has 30+ minutes for the beat.

## Prompt 2 (optional, after the burn-down): the dashboard

```text
Write demo/out/scanner/results.json from the children's STATUS lines in the shape demo/scanner/collect.py writes (one entry per finding, with pr_url and session_url), run demo/scanner/report.py, and attach demo/out/scanner/burndown.html and demo/reports/demo1-remediation-report.md.
```

## Expected outcome (what "it worked" looks like)

| Finding | Repo (host) | Expected status | What Devin does |
|---|---|---|---|
| SNYK-JAVA-ORGYAML-3152153 | settlement-instruction-service (STPS) | **fixed-with-deviation** | Takes core-dates 1.5.0 (scanner path), `CustodyGatewayContractTest` fails (tag 64 becomes `2027-10-13` instead of `20271013`), reverts, pins core-dates **1.4.1** (security-only release, snakeyaml 2.2), adds a test that pins the tag-64 wire format |
| SNYK-JAVA-ORGAPACHECOMMONS-6254296 | custody-gateway (STPS) | fixed | commons-compress 1.24.0 to 1.26.0 through the BOM property or an override |
| SNYK-SECRET-ARDEN-0418 | fx-funding-service (STPS) | fixed | Token removed from `ci/settings.xml`, read from CI env, `ci-shared-workflows` v3 to v4 (OIDC); PR says the token still needs revoking |
| SNYK-CC-ARDEN-TOKENTTL | platform-terraform (Springboot-BankApp) | fixed | `expires_in_seconds` set to a finite TTL with blast-radius note, `terraform fmt` |
| SNYK-CODE-PY-YAMLLOAD-0091 | risk-lib (traderX) | fixed | `yaml.load(..., Loader=yaml.Loader)` to `yaml.safe_load`, hostile-input regression test |

The one hard case is the first row. Devin stops there: it does not force 1.5.0 and does not loosen the contract test.
