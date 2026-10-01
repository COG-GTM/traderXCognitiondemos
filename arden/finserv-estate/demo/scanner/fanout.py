#!/usr/bin/env python3
"""Parent-session step: turn normalised findings into one child session per finding.

    python3 demo/scanner/fanout.py                    # dry-run: writes plan + prompts under demo/out/scanner/
    python3 demo/scanner/fanout.py --mode live        # creates real child sessions via the Devin API (DEVIN_API_KEY)

Live mode uses POST https://api.devin.ai/v1/sessions (see https://docs.devin.ai/api-reference). Each child gets a
prompt scoped to ONE finding in ONE repo with the exact file the scanner pointed at, the repo's build/test commands
and the org playbook macro. It never asks the child to scan the repo itself, and never asks it to run interactive
`gh` commands.
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, log, now_iso, owners_for, read_json, source_dir, write_json  # noqa: E402

PLAYBOOK_MACRO = os.environ.get("FS_REMEDIATION_PLAYBOOK", "@playbook:playbook-fs-cve-remediation-child")
INCIDENT_REF = "SEC-2026-0912 (CVE-2026-82329 Artifactory response)"

BUILD_CMDS = {
    "maven": "mvn -s ci/settings.xml -q verify   (Java 17)",
    "pip": "pip install -e .[test] && python -m pytest",
    "npm": "npm test",
    "terraform": "terraform fmt -check -recursive && terraform validate",
}


def build_system(repo: str) -> str:
    d = source_dir(repo)
    if (d / "pom.xml").exists():
        return "maven"
    if (d / "pyproject.toml").exists():
        return "pip"
    if (d / "package.json").exists():
        return "npm"
    if list(d.glob("**/*.tf")):
        return "terraform"
    return "none"


def child_prompt(f: dict) -> str:
    repo = f["repo"]
    bs = build_system(repo)
    owners = ", ".join(owners_for(source_dir(repo), f["path"].split(":")[0])) or "(no CODEOWNERS match)"
    lines = [
        f"{PLAYBOOK_MACRO}",
        f"Remediate ONE scanner finding in ardencm/{repo} as part of {INCIDENT_REF}.",
        "",
        f"Finding: {f['scanner_id']} — {f['title']} (severity {f['severity']}, {', '.join(f['cve']) or 'no CVE'})",
        f"Location: {f['path']}",
        f"Kind: {f['kind']}",
    ]
    if f["kind"] == "sca":
        lines += [
            f"Vulnerable package: {f['package']}@{f['version']}; fixed in {', '.join(f['fixed_in'])}.",
            f"Dependency chain: {' -> '.join(f['dependency_chain'])}",
            f"Scanner-suggested upgrade path: {' -> '.join(f['upgrade_path'])}",
            "Before taking the scanner's suggested version, read the repo's dependency policy (docs/adr in common-java-bom, "
            "the library's CHANGELOG) and run every consumer contract test (*ContractTest) in the repo. If a contract breaks, "
            "choose the smallest version that fixes the CVE without changing wire formats, and add a test that pins the "
            "wire-format assumption so the next upgrade cannot silently break it.",
        ]
    elif f["kind"] == "secret":
        lines += [
            "Remove the hard-coded Artifactory token from the file, read it from the CI-provided environment instead, and "
            "move the workflow from ci-shared-workflows @v3 (static ARTIFACTORY_TOKEN secret) to @v4 (OIDC exchange). "
            "Do NOT rotate or revoke the token yourself — record the token fingerprint (first 8 chars) and its file path in "
            "the PR description under 'Token revocation required' so security can revoke it in Artifactory.",
        ]
    elif f["kind"] == "iac":
        lines += [
            "Change only the resource the finding points at. Keep the CAB-ticket comment conventions used in the file. "
            "For version pins, cite the vendor advisory in the PR. For token TTLs, propose a value and explain the blast radius.",
        ]
    elif f["kind"] == "sast":
        lines += ["Fix the unsafe call in place, add a regression test that proves the unsafe path is gone, and keep the public API."]
    lines += [
        "",
        f"Build/test: {BUILD_CMDS.get(bs, 'see README')}",
        f"CODEOWNERS for this path: {owners}. Add them as reviewers; do not merge.",
        "Open a PR titled 'security: <finding id> — <one line>' with a body containing: finding id, CVE, files changed, "
        "test evidence (paste the test summary line), and a 'Residual risk' section. Add label 'cve-2026-82329-response'.",
        "Report back to the parent with: PR URL, status (fixed | fixed-with-deviation | blocked), and one sentence why.",
    ]
    return "\n".join(lines)


def create_session(prompt: str, title: str, tags: list[str]) -> dict:
    key = os.environ.get("DEVIN_API_KEY") or sys.exit("DEVIN_API_KEY not set")
    body = json.dumps({"prompt": prompt, "title": title, "tags": tags, "unlisted": False, "idempotent": True}).encode()
    req = urllib.request.Request(os.environ.get("DEVIN_API", "https://api.devin.ai") + "/v1/sessions", data=body,
                                 headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--findings", default=str(OUT / "scanner/findings.json"))
    ap.add_argument("--mode", choices=["dry-run", "live"], default="dry-run")
    ap.add_argument("--max-parallel", type=int, default=8)
    a = ap.parse_args()

    findings = read_json(pathlib.Path(a.findings))
    prompts_dir = OUT / "scanner/prompts"
    prompts_dir.mkdir(parents=True, exist_ok=True)
    children = []
    for i, f in enumerate(findings):
        prompt = child_prompt(f)
        slug = f["id"].replace("/", "__").replace("@", "__at__").replace(":", "_")
        (prompts_dir / f"{i:02d}-{slug}.md").write_text(prompt + "\n")
        child = {"index": i, "finding_id": f["id"], "repo": f["repo"], "severity": f["severity"], "kind": f["kind"],
                 "title": f"[{f['severity']}] {f['repo']}: {f['scanner_id']}", "prompt_file": str(prompts_dir / f"{i:02d}-{slug}.md"),
                 "owners": owners_for(source_dir(f["repo"]), f["path"].split(":")[0]), "session": None}
        if a.mode == "live":
            child["session"] = create_session(prompt, child["title"], ["cve-2026-82329-response", f["repo"], f["kind"]])
            log(f"created {child['session'].get('url', child['session'].get('session_id'))} for {f['id']}")
        children.append(child)

    plan = {
        "parent": {"incident": INCIDENT_REF, "trigger": "scanner webhook (snyk project_snapshot)", "created_at": now_iso(),
                   "mode": a.mode, "max_parallel": a.max_parallel, "policy": "one child session per finding (org rule)"},
        "summary": {"findings": len(findings), "repos": sorted({f["repo"] for f in findings}),
                    "by_severity": {s: sum(f["severity"] == s for f in findings) for s in ("critical", "high", "medium", "low")},
                    "by_kind": {k: sum(f["kind"] == k for f in findings) for k in ("sca", "secret", "iac", "sast")}},
        "children": children,
    }
    write_json(OUT / "scanner/fanout-plan.json", plan)
    log(f"fan-out plan: {len(children)} children across {len(plan['summary']['repos'])} repos ({a.mode}) -> {OUT/'scanner/fanout-plan.json'}")


if __name__ == "__main__":
    main()
