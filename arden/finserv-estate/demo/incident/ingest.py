#!/usr/bin/env python3
"""Demo 3, step 1 — turn a Datadog monitor alert plus its context (error logs, APM trace, deploy events) into one
normalised incident input.  Swappable source: fixtures (deterministic) or the Datadog API (real).

    python3 demo/incident/ingest.py --source fixture                  # demo/incident/fixtures/datadog/*.json
    python3 demo/incident/ingest.py --source datadog --input alert.json   # webhook body + DD_API_KEY/DD_APP_KEY[/DD_SITE]

Output: demo/out/incident/incident-input.json
    { alert: {monitor_id, title, fired_at, query, scope, priority, link, body},
      errors: [{ts, route, message, stack, version, feed_schema}],
      healthy: [{ts, version, feed_schema}],              # last good requests, to bound the change window
      deploys: [{service, version, at, commit, ticket, summary}],
      trace:  {trace_id, spans:[...]} }
Datadog mode uses Logs Search v2 (POST /api/v2/logs/events/search) and Events v1 (GET /api/v1/events) with the
monitor's scope and a window of alert-24h..alert+15m; the fixture files are shaped like those responses.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import pathlib
import sys
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, log, now_iso, read_json, write_json  # noqa: E402

FIX = pathlib.Path(__file__).resolve().parent / "fixtures/datadog"


def iso(ms_or_s: str | int) -> str:
    v = int(ms_or_s)
    if v > 10**11:
        v //= 1000
    return dt.datetime.fromtimestamp(v, dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def normalise_alert(w: dict) -> dict:
    return {"monitor_id": w.get("alert_id"), "event_id": w.get("id"), "title": w.get("title"), "fired_at": iso(w["date"]),
            "transition": w.get("alert_transition"), "query": w.get("alert_query"), "scope": w.get("alert_scope"),
            "priority": w.get("priority"), "link": w.get("link"), "body": w.get("body", ""),
            "tags": [t for t in (w.get("tags") or "").split(",") if t], "service": next((t.split(":", 1)[1] for t in (w.get("tags") or "").split(",") if t.startswith("service:")), None)}


def normalise_logs(resp: dict) -> tuple[list[dict], list[dict]]:
    errors, healthy = [], []
    for item in resp.get("data", []):
        a = item["attributes"]
        inner = a.get("attributes", {})
        version = next((t.split(":", 1)[1] for t in a.get("tags", []) if t.startswith("version:")), None)
        rec = {"ts": a["timestamp"], "route": inner.get("route"), "version": version, "feed_schema": (inner.get("feed") or {}).get("schema")}
        if a.get("status") == "error":
            err = inner.get("error") or {}
            rec.update(message=err.get("message") or a.get("message"), kind=err.get("kind"), stack=err.get("stack", ""))
            errors.append(rec)
        else:
            healthy.append(rec)
    return sorted(errors, key=lambda r: r["ts"]), sorted(healthy, key=lambda r: r["ts"])


def normalise_events(resp: dict) -> list[dict]:
    out = []
    for e in resp.get("events", []):
        tags = dict(t.split(":", 1) for t in e.get("tags", []) if ":" in t)
        out.append({"service": tags.get("service"), "version": tags.get("version"), "at": iso(e["date_happened"]), "commit": tags.get("commit"),
                    "ticket": tags.get("ticket"), "summary": e.get("text", e.get("title"))})
    return sorted(out, key=lambda d: d["at"], reverse=True)


def dd_request(method: str, path: str, body: dict | None = None) -> dict:
    site = os.environ.get("DD_SITE", "datadoghq.com")
    req = urllib.request.Request(f"https://api.{site}{path}", method=method, data=json.dumps(body).encode() if body else None,
                                 headers={"DD-API-KEY": os.environ["DD_API_KEY"], "DD-APPLICATION-KEY": os.environ["DD_APP_KEY"], "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def from_datadog(alert: dict) -> tuple[dict, dict]:
    fired = dt.datetime.strptime(alert["fired_at"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=dt.timezone.utc)
    start, end = fired - dt.timedelta(hours=24), fired + dt.timedelta(minutes=15)
    logs = dd_request("POST", "/api/v2/logs/events/search", {"filter": {"query": f"service:{alert['service']} @route:*valuation*", "from": start.isoformat(), "to": end.isoformat()},
                                                             "sort": "timestamp", "page": {"limit": 500}})
    events = dd_request("GET", f"/api/v1/events?start={int(start.timestamp())}&end={int(end.timestamp())}&sources=deploy&tags=env:prod")
    return logs, events


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", choices=["fixture", "datadog"], default=os.environ.get("FS_INCIDENT_SOURCE", "fixture"))
    ap.add_argument("--input", help="webhook body (JSON) — defaults to the fixture")
    a = ap.parse_args()
    webhook = read_json(pathlib.Path(a.input)) if a.input else read_json(FIX / "monitor-webhook.json")
    alert = normalise_alert(webhook)
    if a.source == "datadog":
        if not (os.environ.get("DD_API_KEY") and os.environ.get("DD_APP_KEY")):
            raise SystemExit("DD_API_KEY and DD_APP_KEY are required for --source datadog")
        logs_resp, events_resp = from_datadog(alert)
        trace = {}
    else:
        logs_resp, events_resp, trace = read_json(FIX / "logs-search.json"), read_json(FIX / "deploy-events.json"), read_json(FIX / "apm-error-trace.json")
    errors, healthy = normalise_logs(logs_resp)
    deploys = normalise_events(events_resp)
    out = {"source": a.source, "ingested_at": now_iso(), "alert": alert, "errors": errors, "healthy": healthy, "deploys": deploys, "trace": trace}
    write_json(OUT / "incident/incident-input.json", out)
    svc_deploys = [d for d in deploys if d["service"] == alert["service"]]
    log(f"incident input: alert {alert['monitor_id']} fired {alert['fired_at']} · {len(errors)} error logs (first {errors[0]['ts'] if errors else '-'}) · "
        f"{len(healthy)} healthy (last {healthy[-1]['ts'] if healthy else '-'}) · {len(svc_deploys)} deploys of {alert['service']} in window (latest {svc_deploys[0]['at'] if svc_deploys else '-'})")


if __name__ == "__main__":
    main()
