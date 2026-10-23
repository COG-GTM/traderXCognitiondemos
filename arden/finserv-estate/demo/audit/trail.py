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
import html
import json
import os
import re
import urllib.error
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


def build(chain: dict, ticket: dict, log: dict, label: str | None = None) -> list[tuple[str, str, str]]:
    if label is None:
        captured = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())["_captured"]
        label = f"Recorded audit snapshot (fixture), captured {captured}"
    sid = chain["devin_session"].rstrip("/").rsplit("/", 1)[-1]
    session_events = [e for e in log.get("items", []) if sid in json.dumps(e.get("data") or {})]
    started = next((e for e in session_events if e["action"] == "create_session"), None)
    st = chain["pr_state"]
    snap = f"PR snapshot {chain['pr_state_observed']}; check the live PR"

    def ts(e): return datetime.fromtimestamp(e["created_at"], timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    return [
        ("Why", f"{ticket['key']}: {ticket['summary']}",
         f"{ticket['key']} ticket (recorded fixture), reported by {ticket['reporter']}"),
        ("Who asked Devin", f"{started['user_email']} at {ts(started)}" if started else "MISSING",
         f"{label}: Devin audit log create_session" if started else f"{label}: no audit-log record for this session"),
        ("What Devin did", "\n".join(chain["commands_run_by_devin"]),
         chain["devin_session"]),
        ("The change", f"{chain['commit']} {chain['commit_message']}",
         f"{chain['pr']} ({chain['pr_footer']}), {chain['commit_url']}"),
        ("CI", st["ci"],
         f"{snap}: security/snyk commit status on {chain['commit_url']}, {chain['ci_status_url']}"),
        ("Approved by", st["approved_by"] or f"not approved yet; required: {', '.join(chain['required_reviewers'])}", f"{snap}: reviews"),
        ("Deployed", "yes" if st["deployed"] else "no (not merged, not applied)", f"{snap}: merge state"),
    ]


def _markdown_links(text: str) -> str:
    return re.sub(r"(https?://[^\s)]+)", lambda m: f"[{m.group(1)}]({m.group(1)})", text)


def _chain_url_labels(chain: dict) -> dict[str, str]:
    session_id = chain["devin_session"].rstrip("/").rsplit("/", 1)[-1]
    return {
        chain["pr"]: f"PR #{chain['pr'].rstrip('/').rsplit('/', 1)[-1]}",
        chain["commit_url"]: f"commit {chain['commit'][:7]}",
        chain["ci_status_url"]: "Snyk report",
        chain["devin_session"]: f"Devin session {session_id[:8]}",
    }


def _html_links(text: str, labels: dict[str, str]) -> str:
    parts = []
    position = 0
    for match in re.finditer(r"""https?://[^\s<>"')]+""", text):
        parts.append(html.escape(text[position:match.start()]))
        url = match.group(0)
        suffix = ""
        while url and url[-1] in ",.;:" and url not in labels:
            suffix = url[-1] + suffix
            url = url[:-1]
        label = html.escape(labels.get(url, url))
        parts.append(f'<a href="{html.escape(url, quote=True)}">{label}</a>')
        parts.append(html.escape(suffix))
        position = match.end()
    parts.append(html.escape(text[position:]))
    return "".join(parts)


def _markdown_record(question: str, record: str, ticket: dict) -> str:
    if question != "Why":
        return _markdown_links(record)
    label = f"{ticket['key']} ticket (recorded fixture)"
    suffix = record[len(label):]
    return f"[{label}](../../audit/fixtures/trace/{ticket['key']}.json){suffix}"


def _html_record(question: str, record: str, ticket: dict, labels: dict[str, str]) -> str:
    if question != "Why":
        return _html_links(record, labels)
    label = f"{ticket['key']} ticket (recorded fixture)"
    suffix = html.escape(record[len(label):])
    href = html.escape(f"../../audit/fixtures/trace/{ticket['key']}.json", quote=True)
    return f'<a href="{href}">{html.escape(label)}</a>{suffix}'


def _fallback_reason(exc: Exception) -> str:
    if isinstance(exc, RuntimeError):
        return str(exc)
    if isinstance(exc, urllib.error.HTTPError):
        if exc.code == 403:
            return "HTTP 403 Forbidden (the token needs ViewAccountAuditLogs)"
        return f"HTTP {exc.code} {exc.reason}"
    if isinstance(exc, urllib.error.URLError):
        return f"network error: {exc.reason}"
    if isinstance(exc, TimeoutError):
        return f"network error: {exc}"
    if isinstance(exc, json.JSONDecodeError):
        return f"network error: {exc}"
    return f"network error: {exc}"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--live", action="store_true")
    ap.add_argument("--out", type=Path, default=HERE.parent / "out" / "audit")
    a = ap.parse_args()
    chain = json.loads((HERE / "fixtures/trace/chain.json").read_text())
    ticket = json.loads((HERE / f"fixtures/trace/{chain['ticket']}.json").read_text())
    fixture = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    SNAPSHOT = f"Recorded audit snapshot (fixture), captured {fixture['_captured']}"
    label = SNAPSHOT
    log = fixture
    if a.live:
        try:
            org = os.environ.get("DEVIN_ORG_ID")
            token = os.environ.get("DEVIN_AUDIT_TOKEN") or os.environ.get("DEVIN_API_KEY")
            if not org:
                raise RuntimeError("DEVIN_ORG_ID is not set")
            if not token:
                raise RuntimeError("DEVIN_AUDIT_TOKEN is not set")
            log = fetch_live(org, token, fixture["items"][0]["created_at"])
            label = f"Live Devin audit log, fetched {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}"
        except (RuntimeError, urllib.error.HTTPError, urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            print(f"Live audit log unavailable: {_fallback_reason(exc)}. Showing recorded snapshot.")
            log = fixture
            label = SNAPSHOT
    rows = build(chain, ticket, log, label)
    a.out.mkdir(parents=True, exist_ok=True)
    (a.out / "devin-audit-log.json").write_text(json.dumps(log, indent=2))
    md = ["# Traceability: who changed the Artifactory token lifetime, why, and who approved it", "",
          f"Devin audit log source: {label}. Endpoint: `GET /v3/enterprise/organizations/{{org_id}}/audit-logs` "
          "(https://docs.devin.ai/api-reference/v3/audit-logs/organizations-audit-logs).", "",
          "| Question | Answer | Record |", "|---|---|---|"]
    md.extend(f"| {q} | {v.replace(chr(10), '<br>')} | {_markdown_record(q, r, ticket)} |" for q, v, r in rows)
    (a.out / "traceability.md").write_text("\n".join(md) + "\n")
    banner_style = "background:#fff1cf;border:1px solid #d89b22;padding:10px;margin:12px 0" if "fixture" in label else "background:#e8f5ec;border:1px solid #6a7;padding:10px;margin:12px 0"
    url_labels = _chain_url_labels(chain)
    html_rows = "".join(
        f"<tr><td>{html.escape(q)}</td><td>{html.escape(v).replace(chr(10), '<br>')}</td>"
        f"<td>{_html_record(q, r, ticket, url_labels)}</td></tr>"
        for q, v, r in rows
    )
    (a.out / "traceability.html").write_text(
        "<!doctype html><meta charset=utf-8><title>Change trail</title>"
        "<style>body{font:15px system-ui;margin:32px}table{border-collapse:collapse;width:100%;table-layout:fixed}"
        "td,th{border:1px solid #ccc;padding:8px;vertical-align:top;word-break:break-word}"
        "th{background:#f3f3f3;text-align:left}a{overflow-wrap:anywhere}</style>"
        "<h2>Change trail: CN13 token lifetime fix</h2>"
        f"<div style='{banner_style}'>{html.escape(label)}</div>"
        "<p><a href='control-map.html'>Back to control map</a></p>"
        f"<table><tr><th>Question</th><th>Answer</th><th>Record</th></tr>{html_rows}</table>"
    )
    print(f"  Audit log source: {label}")
    for q, v, r in rows:
        values = v.splitlines()
        print(f"  {q:<16} {values[0]}")
        for line in values[1:]:
            print(f"  {'':<16} {line}")
        print(f"  {'':<16} └ {r}")


if __name__ == "__main__":
    main()
