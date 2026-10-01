#!/usr/bin/env python3
"""Live-mode companion to fanout.py: poll the child sessions and write results.json in the same shape the
deterministic executor produces, so report.py works unchanged.

Children end with one line:  STATUS=<s> PR=<url> TESTS="<summary>" WHY="<sentence>"
"""
from __future__ import annotations

import json
import os
import pathlib
import re
import sys
import time
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, log, now_iso, read_json, write_json  # noqa: E402

STATUS_LINE = re.compile(r'STATUS=(\S+)\s+PR=(\S+)\s+TESTS="([^"]*)"\s+WHY="([^"]*)"')


def get_session(session_id: str) -> dict:
    key = os.environ.get("DEVIN_API_KEY") or sys.exit("DEVIN_API_KEY not set")
    req = urllib.request.Request(os.environ.get("DEVIN_API", "https://api.devin.ai") + f"/v1/session/{session_id}",
                                 headers={"Authorization": f"Bearer {key}"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def last_devin_message(session: dict) -> str:
    msgs = [m for m in session.get("messages", []) if m.get("type") == "devin_message"]
    return msgs[-1]["message"] if msgs else ""


def main() -> None:
    plan = read_json(OUT / "scanner/fanout-plan.json")
    pending = {c["finding_id"]: c for c in plan["children"] if c.get("session")}
    results: dict[str, dict] = {}
    deadline = time.time() + int(os.environ.get("FS_COLLECT_TIMEOUT", "3600"))
    while pending and time.time() < deadline:
        for fid, c in list(pending.items()):
            s = get_session(c["session"]["session_id"])
            m = STATUS_LINE.search(last_devin_message(s))
            if s.get("status_enum") in ("finished", "blocked", "expired") or m:
                status, pr, tests, why = m.groups() if m else ("blocked", "", "", f"session {s.get('status_enum')} without status line")
                results[fid] = {"finding_id": fid, "repo": c["repo"], "severity": c["severity"], "kind": c["kind"],
                                "branch": "", "status": status, "pr_url": pr, "owners": c["owners"],
                                "attempts": [{"strategy": "live child session", "changes": [], "tests_ok": status != "blocked",
                                              "test_summary": tests, "why": why}],
                                "started_at": c["session"].get("created_at", plan["parent"]["created_at"]), "finished_at": now_iso(),
                                "pr_dir": "", "session_url": c["session"].get("url", "")}
                log(f"{c['repo']:32s} {status:22s} {pr}")
                del pending[fid]
        if pending:
            time.sleep(30)
    for fid, c in pending.items():
        results[fid] = {"finding_id": fid, "repo": c["repo"], "severity": c["severity"], "kind": c["kind"], "branch": "",
                        "status": "blocked", "owners": c["owners"], "attempts": [{"strategy": "live child session", "changes": [],
                        "tests_ok": False, "test_summary": "timed out waiting for child", "why": ""}],
                        "started_at": plan["parent"]["created_at"], "finished_at": now_iso(), "pr_dir": ""}
    write_json(OUT / "scanner/results.json", list(results.values()))
    log(f"collected {len(results)} results -> {OUT / 'scanner/results.json'}")


if __name__ == "__main__":
    main()
