"""Answer the auditor's question for one change: who changed it, why, through which Devin session, and who approved it.

    python3 demo/audit/trail.py                     # offline: fixtures/trace/*.json (devin-audit-log.json is a schema-accurate fixture)
    DEVIN_AUDIT_TOKEN=... DEVIN_ORG_ID=org_... python3 demo/audit/trail.py --live
        # pulls the real Devin audit log: GET https://api.devin.ai/v3/enterprise/organizations/{org_id}/audit-logs
        # (docs: https://docs.devin.ai/api-reference/v3/audit-logs/organizations-audit-logs). Needs ViewAccountAuditLogs.

The trail only states what a record proves. A link with no record behind it is printed as MISSING and nothing is
back-filled.
"""
from __future__ import annotations

import argparse
import json
import os
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
API = "https://api.devin.ai/v3/enterprise/organizations/{org}/audit-logs"
WINDOW = 100 * 24 * 3600  # documented maximum for time_after..time_before


def fetch_live(org: str, token: str, around: int) -> dict:
    q = urllib.parse.urlencode({"time_after": around - WINDOW // 2, "time_before": around + WINDOW // 2, "first": 200, "order": "asc"})
    items, after = [], None
    while True:
        page_q = q + (f"&after={urllib.parse.quote(after)}" if after else "")
        req = urllib.request.Request(f"{API.format(org=org)}?{page_q}", headers={"Authorization": f"Bearer {token}"})
        with urllib.request.urlopen(req, timeout=30) as r:  # any failure aborts: a partial export is never shown
            page = json.load(r)
        items += page.get("items", [])
        if not page.get("has_next_page"):
            return {"items": items, "end_cursor": None, "has_next_page": False, "total": len(items)}
        after = page["end_cursor"]


def build(chain: dict, ticket: dict, log: dict) -> list[tuple[str, str, str]]:
    sid = chain["devin_session"].rstrip("/").rsplit("/", 1)[-1]
    session_events = [e for e in log.get("items", []) if sid in json.dumps(e.get("data") or {})]
    started = next((e for e in session_events if e["action"] == "create_session"), None)
    st = chain["pr_state"]
    snap = f"PR snapshot {chain['pr_state_observed']}; check the live PR"
    def ts(e): return datetime.fromtimestamp(e["created_at"], timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    return [
        ("Why", f"{ticket['key']}: {ticket['summary']}", f"ticket, reported by {ticket['reporter']}"),
        ("Who asked Devin", f"{started['user_email']} at {ts(started)}" if started else "MISSING",
         "Devin audit log `create_session`" if started else "no audit-log record for this session"),
        ("What Devin did", "; ".join(chain["commands_run_by_devin"]), f"session transcript {chain['devin_session']}"),
        ("The change", f"{chain['commit']} {chain['commit_message']}", f"PR {chain['pr']} ({chain['pr_footer']})"),
        ("CI", st["ci"], f"{snap}: {chain['pr']}/checks"),
        ("Approved by", st["approved_by"] or f"not approved yet; required: {', '.join(chain['required_reviewers'])}", f"{snap}: reviews"),
        ("Deployed", "yes" if st["deployed"] else "no (not merged, not applied)", f"{snap}: merge state"),
    ]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--live", action="store_true")
    ap.add_argument("--out", type=Path, default=HERE.parent / "out" / "audit")
    a = ap.parse_args()
    chain = json.loads((HERE / "fixtures/trace/chain.json").read_text())
    ticket = json.loads((HERE / f"fixtures/trace/{chain['ticket']}.json").read_text())
    fixture = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    if a.live:
        log, source = fetch_live(os.environ["DEVIN_ORG_ID"], os.environ["DEVIN_AUDIT_TOKEN"], fixture["items"][0]["created_at"]), "live API"
    else:
        log, source = fixture, "fixture (documented v3 schema)"
    rows = build(chain, ticket, log)
    a.out.mkdir(parents=True, exist_ok=True)
    (a.out / "devin-audit-log.json").write_text(json.dumps(log, indent=2))
    md = ["# Traceability: who changed the Artifactory token lifetime, why, and who approved it", "",
          f"Devin audit log source: {source}. Endpoint: `GET /v3/enterprise/organizations/{{org_id}}/audit-logs` "
          "(https://docs.devin.ai/api-reference/v3/audit-logs/organizations-audit-logs).", "",
          "| Question | Answer | Record |", "|---|---|---|"] + [f"| {q} | {v} | {r} |" for q, v, r in rows]
    (a.out / "traceability.md").write_text("\n".join(md) + "\n")
    for q, v, r in rows:
        print(f"  {q:<16} {v}\n  {'':<16} └ {r}")


if __name__ == "__main__":
    main()
