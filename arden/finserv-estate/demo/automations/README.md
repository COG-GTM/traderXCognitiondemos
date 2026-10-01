# Automations — how each demo is triggered (bindings, not code; the receivers live in demo/)

| Demo | Event | Receiver in this estate | Devin binding |
|---|---|---|---|
| 1 | Snyk webhook `project_snapshot/v0` (or SonarQube webhook, or a Snyk API poll) | `demo/scanner/webhook.py` (HMAC-validated, port 8787) → normalize → fan-out | Webhook automation → **parent** session runs `demo/run-demo1.sh`-equivalent; **one child per finding** via `POST /v1/sessions` with `@playbook:playbook-fs-cve-remediation-child` (`FS_FANOUT_MODE=live`, `DEVIN_API_KEY`) |
| 2 | Scheduled (programme milestone) or manual | `demo/run-demo2.sh` | Session with `@playbook:playbook-fs-t1-migration-tranche` and the programme ticket + go-live date + tranche as inputs |
| 3 | Datadog monitor webhook → Slack `#inc-client-portal` | `demo/incident/ingest.py --source datadog` (`DD_API_KEY`, `DD_APP_KEY`) → investigate → report | Slack-channel or webhook automation → session with `@playbook:playbook-fs-incident-response`; thread replies via `FS_SLACK_MODE=live` |

Deterministic mode needs none of the credentials: fixtures under `demo/scanner/fixtures/` and `demo/incident/fixtures/datadog/` are shaped like the real payloads, and the same code path runs.

Repos: the estate repos are `arden/<repo>` sub-trees of COG-GTM repos (`scripts/repo-map.tsv`); the automations clone the hosting repo and work under that sub-tree. PRs are opened on the hosting repo (`scripts/publish-branch.sh`).

Playbook IDs: upload `demo/playbooks/*.md` to the org and substitute the generated `playbook-<id>` for the names above.
Knowledge: upload `demo/knowledge/*.md` with the scopes in their first line.
