#!/usr/bin/env python3
"""Normalise scanner output into the estate's finding schema.

    python3 demo/scanner/normalize.py --source snyk-webhook --input demo/scanner/fixtures/snyk-webhook.json
    python3 demo/scanner/normalize.py --source snyk-api --org <org_id>            # live: needs SNYK_TOKEN
    python3 demo/scanner/normalize.py --source sonarqube --input export.json      # adapter, see adapters/

Output: demo/out/scanner/findings.json (list[Finding]). One Finding == one child session (org rule:
one session per vulnerability, never grouped).

Finding schema
--------------
    id            stable id: "<repo>/<scanner_issue_id>[@<path>]"
    repo          estate repo name (owner prefix stripped)
    scanner       "snyk" | "sonarqube" | ...
    scanner_id    the scanner's issue id
    kind          "sca" | "secret" | "iac" | "sast"
    severity      critical | high | medium | low
    title, cve[], cwe[], cvss
    package, version, fixed_in[], upgrade_path[]      (sca)
    path          file[:line] the scanner pointed at
    notes
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, log, write_json  # noqa: E402

KIND_BY_SNYK_TYPE = {"vuln": "sca", "license": "sca", "secret": "secret", "configuration": "iac", "code": "sast"}


def repo_from_project_name(name: str) -> str:
    return name.split(":", 1)[0].split("/")[-1]


def from_snyk_webhook(payload: dict) -> list[dict]:
    out = []
    for proj in payload["projects"]:
        repo = repo_from_project_name(proj["name"])
        for issue in proj["issues"]:
            path = (issue.get("path") or [proj.get("targetFile", "")])[0]
            ids = issue.get("identifiers", {})
            out.append({
                "id": f"{repo}/{issue['id']}" + (f"@{path}" if issue["issueType"] != "vuln" else ""),
                "repo": repo,
                "scanner": "snyk",
                "scanner_id": issue["id"],
                "kind": KIND_BY_SNYK_TYPE[issue["issueType"]],
                "severity": issue["severity"],
                "title": issue["title"],
                "cve": ids.get("CVE", []),
                "cwe": ids.get("CWE", []),
                "cvss": issue.get("cvssScore"),
                "package": issue.get("package"),
                "version": issue.get("version"),
                "fixed_in": issue.get("fixedIn", []),
                "upgrade_path": [p for p in issue.get("upgradePath", []) if p],
                "dependency_chain": issue.get("from", []),
                "path": path,
                "notes": issue.get("notes", ""),
                "received_at": payload.get("receivedAt"),
            })
    return out


def from_snyk_api(org_id: str) -> list[dict]:
    """Live adapter: REST issues endpoint. Token from SNYK_TOKEN. Maps the REST shape onto the webhook mapper."""
    token = os.environ.get("SNYK_TOKEN") or sys.exit("SNYK_TOKEN not set")
    base = os.environ.get("SNYK_API", "https://api.snyk.io")
    url = f"{base}/rest/orgs/{org_id}/issues?version=2024-10-15&limit=100"
    projects: dict[str, dict] = {}
    while url:
        req = urllib.request.Request(url, headers={"Authorization": f"token {token}", "Accept": "application/vnd.api+json"})
        with urllib.request.urlopen(req, timeout=30) as r:
            page = json.load(r)
        for item in page["data"]:
            a = item["attributes"]
            scan = item["relationships"]["scan_item"]["data"]
            proj = projects.setdefault(scan["id"], {"id": scan["id"], "name": a.get("title", scan["id"]), "issues": []})
            coords = (a.get("coordinates") or [{}])[0]
            rep = (coords.get("representations") or [{}])[0]
            dep = rep.get("dependency", {})
            proj["issues"].append({
                "id": a.get("key", item["id"]),
                "issueType": {"package_vulnerability": "vuln", "code": "code", "cloud": "configuration", "config": "configuration"}.get(a["type"], "vuln"),
                "title": a["title"], "severity": a["effective_severity_level"],
                "identifiers": {"CVE": [p["id"] for p in a.get("problems", []) if p["source"] == "CVE"],
                                "CWE": [c["id"] for c in a.get("classes", []) if c["source"] == "CWE"]},
                "package": dep.get("package_name"), "version": dep.get("package_version"),
                "isUpgradable": coords.get("is_upgradeable", False),
                "fixedIn": [], "upgradePath": [], "from": [],
                "path": [rep.get("sourceLocation", {}).get("file", "")],
            })
        url = page.get("links", {}).get("next")
        if url and url.startswith("/"):
            url = base + url
    return from_snyk_webhook({"projects": list(projects.values()), "receivedAt": None})


def from_sonarqube(payload: dict) -> list[dict]:
    """Adapter for `GET /api/hotspots/search` + `GET /api/issues/search` exports (see adapters/sonarqube.md)."""
    out = []
    for iss in payload.get("issues", []) + payload.get("hotspots", []):
        repo = iss["project"].split(":")[-1]
        sev = {"BLOCKER": "critical", "CRITICAL": "critical", "MAJOR": "high", "MINOR": "medium", "INFO": "low",
               "HIGH": "critical", "MEDIUM": "high", "LOW": "medium"}[iss.get("severity") or iss.get("vulnerabilityProbability")]
        path = iss["component"].split(":", 1)[-1] + (f":{iss['line']}" if iss.get("line") else "")
        out.append({"id": f"{repo}/{iss['key']}@{path}", "repo": repo, "scanner": "sonarqube", "scanner_id": iss["key"],
                    "kind": "secret" if "credential" in iss.get("message", "").lower() else "sast",
                    "severity": sev, "title": iss["message"], "cve": [], "cwe": [], "cvss": None, "package": None, "version": None,
                    "fixed_in": [], "upgrade_path": [], "dependency_chain": [], "path": path, "notes": iss.get("rule", ""), "received_at": None})
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", choices=["snyk-webhook", "snyk-api", "sonarqube"], default="snyk-webhook")
    ap.add_argument("--input", default=str(pathlib.Path(__file__).parent / "fixtures/snyk-webhook.json"))
    ap.add_argument("--org")
    ap.add_argument("--out", default=str(OUT / "scanner/findings.json"))
    a = ap.parse_args()

    if a.source == "snyk-api":
        findings = from_snyk_api(a.org or os.environ.get("SNYK_ORG_ID") or sys.exit("--org or SNYK_ORG_ID required"))
    else:
        payload = json.loads(pathlib.Path(a.input).read_text())
        findings = from_snyk_webhook(payload) if a.source == "snyk-webhook" else from_sonarqube(payload)

    findings.sort(key=lambda f: ({"critical": 0, "high": 1, "medium": 2, "low": 3}[f["severity"]], f["repo"], f["id"]))
    write_json(pathlib.Path(a.out), findings)
    by_kind: dict[str, int] = {}
    for f in findings:
        by_kind[f["kind"]] = by_kind.get(f["kind"], 0) + 1
    log(f"{len(findings)} findings across {len({f['repo'] for f in findings})} repos from {a.source}: {by_kind} -> {a.out}")


if __name__ == "__main__":
    main()
