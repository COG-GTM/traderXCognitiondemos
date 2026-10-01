#!/usr/bin/env python3
"""Deterministic remediation executor — the fixture fallback for the child sessions.

In a live demo each finding is worked by a real Devin child session (fanout.py --mode live). This script does the
same work locally and deterministically so the estate can be dry-run without Devin, and so the AE can show exactly
what the children *should* land on. For each finding it:

  1. branches the workspace repo (security/<finding-slug>)
  2. applies the fix (per finding kind), running the repo's tests after each attempt
  3. records every attempt — including the failing one for the hard case — in demo/out/scanner/results/<slug>.json
  4. writes the PR-equivalent as a patch + PR body under demo/out/scanner/prs/<slug>/

Hard case (settlement-instruction-service): the scanner recommends core-dates 1.5.0. That version changes FIX tag 64
from LocalMktDate to ISO-8601 and the custody-gateway consumer contract test fails. The executor then pins 1.4.1
(security patch only, per ADR 0007), adds a wire-format pin test, and records the deviation.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import shutil
import sys
import textwrap
import xml.etree.ElementTree as ET

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, WORKSPACE, git, log, now_iso, read_json, run, target_line, write_json  # noqa: E402

PIN_TEST = textwrap.dedent("""\
    package com.ardencm.settlements.instruction;

    import static org.junit.jupiter.api.Assertions.assertTrue;

    import com.ardencm.posttrade.dates.FixDates;
    import java.time.LocalDate;
    import org.junit.jupiter.api.Test;

    /**
     * Pins the FIX tag 64 wire format produced by core-dates. custody-gateway parses tag 64 as LocalMktDate (yyyyMMdd);
     * core-dates 1.5.0 changed FixDates.tag64 to ISO-8601 and broke the consumer contract. Any core-dates upgrade that
     * changes this format must be coordinated with @ardencm/custody-integration (ADR 0007, common-java-bom).
     */
    class Tag64WireFormatPinTest {
        @Test
        void tag64StaysLocalMktDate() {
            String tag64 = FixDates.tag64(LocalDate.of(2027, 10, 13));
            assertTrue(tag64.matches("^[0-9]{8}$"), "tag 64 must be yyyyMMdd, got " + tag64);
        }
    }
    """)


def slug_of(finding: dict) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "-", finding["id"]).strip("-").lower()


def java_summary(repo: pathlib.Path) -> str:
    """Sum the surefire XML reports (mvn -q suppresses the console summary)."""
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed: list[str] = []
    for xml in sorted((repo / "target/surefire-reports").glob("TEST-*.xml")):
        root = ET.parse(xml).getroot()
        for k in totals:
            totals[k] += int(root.get(k, "0"))
        for tc in root.iter("testcase"):
            if any(child.tag in ("failure", "error") for child in tc):
                failed.append(f"{tc.get('classname', '').rsplit('.', 1)[-1]}.{tc.get('name')}")
    s = f"Tests run: {totals['tests']}, Failures: {totals['failures']}, Errors: {totals['errors']}, Skipped: {totals['skipped']}"
    return s + (" — FAILED: " + ", ".join(failed) if failed else "")


def test_repo(repo: pathlib.Path) -> tuple[bool, str, str]:
    if (repo / "pom.xml").exists():
        shutil.rmtree(repo / "target/surefire-reports", ignore_errors=True)
        r = run(["mvn", "-q", "-o", "test"], repo)
        if not r.ok and "offline mode" in r.stdout:
            r = run(["mvn", "-q", "test"], repo)
        if (repo / "target/surefire-reports").exists():
            return r.ok, java_summary(repo), r.stdout
        return r.ok, "BUILD FAILED: " + (r.stdout.strip().splitlines() or [""])[0][:160], r.stdout
    if (repo / "pyproject.toml").exists():
        r = run([sys.executable, "-m", "pytest", "-q"], repo)
        return r.ok, r.stdout.strip().splitlines()[-1] if r.stdout.strip() else "", r.stdout
    if (repo / "package.json").exists():
        r = run(["npm", "test", "--silent"], repo)
        summary = " ".join(l for l in r.stdout.splitlines() if l.startswith("# pass") or l.startswith("# fail"))
        return r.ok, summary, r.stdout
    if list(repo.glob("**/*.tf")):
        tf = shutil.which("terraform") or str(pathlib.Path.home() / ".local/bin/terraform")
        if not pathlib.Path(tf).exists():
            return False, "terraform not installed (run scripts/reset.sh)", ""
        r = run([tf, "fmt", "-check", "-recursive", "-diff"], repo)
        return r.ok, "terraform fmt -check " + ("ok" if r.ok else "FAILED: " + r.stdout.strip().splitlines()[0][:120]), r.stdout
    return True, "no automated tests in repo (see README)", ""


def set_core_dates_version(pom: pathlib.Path, version: str) -> None:
    text = pom.read_text()
    block = re.compile(r"(<artifactId>core-dates</artifactId>\n)(\s*<version>[^<]+</version>\n)?")
    text = block.sub(lambda m: m.group(1) + m.group(1).replace("<artifactId>core-dates</artifactId>", f"<version>{version}</version>"), text, count=1)
    pom.write_text(text)


# -------------------------------------------------------------------------------------------------
# fixers: each returns a list of attempts; an attempt is {"strategy", "changes", "tests_ok", "test_summary"}
# -------------------------------------------------------------------------------------------------
def fix_sca_core_dates(repo: pathlib.Path, f: dict) -> list[dict]:
    attempts = []
    pom = repo / "pom.xml"
    suggested = [p for p in f["upgrade_path"] if p.startswith("com.ardencm.posttrade:core-dates@")][0].split("@")[1]
    set_core_dates_version(pom, suggested)
    ok, summary, _ = test_repo(repo)
    attempts.append({"strategy": f"scanner upgrade path: core-dates {suggested}", "changes": ["pom.xml"], "tests_ok": ok, "test_summary": summary})
    if ok:
        return attempts
    # Hard case: consumer contract broke. Read the library changelog, pick the security-only patch, pin it, add a wire-format test.
    git(repo, "checkout", "--", "pom.xml")
    set_core_dates_version(pom, "1.4.1")
    test_dir = repo / "src/test/java/com/ardencm/settlements/instruction"
    test_dir.mkdir(parents=True, exist_ok=True)
    (test_dir / "Tag64WireFormatPinTest.java").write_text(PIN_TEST)
    ok2, summary2, _ = test_repo(repo)
    attempts.append({"strategy": "deviation: pin core-dates 1.4.1 (SnakeYAML 2.2, no wire change) per ADR 0007 + compensating wire-format test",
                     "changes": ["pom.xml", "src/test/java/com/ardencm/settlements/instruction/Tag64WireFormatPinTest.java"],
                     "tests_ok": ok2, "test_summary": summary2,
                     "reason": "core-dates 1.5.0 changes FixDates.tag64 from LocalMktDate (yyyyMMdd) to ISO-8601; custody-gateway contract test failed."})
    return attempts


def fix_sca_commons_compress(repo: pathlib.Path, f: dict) -> list[dict]:
    pom = repo / "pom.xml"
    fixed = f["fixed_in"][0]
    text = pom.read_text()
    text = text.replace("<artifactId>commons-compress</artifactId>\n", f"<artifactId>commons-compress</artifactId>\n      <version>{fixed}</version>\n", 1)
    pom.write_text(text)
    ok, summary, _ = test_repo(repo)
    return [{"strategy": f"override BOM-managed commons-compress to {fixed} (BOM bump raised separately with @ardencm/platform-eng)",
             "changes": ["pom.xml"], "tests_ok": ok, "test_summary": summary}]


def fix_secret(repo: pathlib.Path, f: dict) -> list[dict]:
    path = repo / f["path"].split(":")[0]
    text = path.read_text()
    token = re.search(r"AKCp_DEMO_FIXTURE_NOT_A_REAL_TOKEN_[A-Za-z0-9_]+", text)
    fingerprint = token.group(0)[:8] + "…" if token else "n/a"
    changes = [f["path"].split(":")[0]]
    if path.name == "settings.xml":
        text = re.sub(r"\s*<!-- TODO\(2024-11\).*?-->\n", "\n", text)
        text = re.sub(r"<password>AKCp_[^<]+</password>", "<password>${env.ARTIFACTORY_TOKEN}</password>", text)
    elif path.name == "pip.conf":
        text = re.sub(r"svc-risk-lib:AKCp_[^@]+@", "", text)
        text += "# Credentials come from PIP_INDEX_URL / keyring in CI (OIDC-issued token); never commit them.\n"
    elif path.name == ".npmrc":
        text = re.sub(r"_authToken=AKCp_[^\n]+", "_authToken=${ARTIFACTORY_TOKEN}", text)
    path.write_text(text)
    wf = repo / ".github/workflows/ci.yml"
    if wf.exists():
        wtext = wf.read_text().replace("ci-shared-workflows/.github/workflows/java-build.yml@v3", "ci-shared-workflows/.github/workflows/java-build.yml@v4")
        wtext = wtext.replace("ci-shared-workflows/.github/workflows/python-test.yml@v3", "ci-shared-workflows/.github/workflows/python-test.yml@v4")
        wtext = wtext.replace("ci-shared-workflows/.github/workflows/node-test.yml@v3", "ci-shared-workflows/.github/workflows/node-test.yml@v4")
        wtext = re.sub(r"\n    secrets:\n      ARTIFACTORY_TOKEN: \$\{\{ secrets.ARTIFACTORY_TOKEN \}\}\n", "\n", wtext)
        wf.write_text(wtext)
        changes.append(".github/workflows/ci.yml")
    ok, summary, _ = test_repo(repo)
    return [{"strategy": "remove committed token, read from CI env; move workflow to ci-shared-workflows @v4 (OIDC)",
             "changes": changes, "tests_ok": ok, "test_summary": summary,
             "revocation_required": {"fingerprint": fingerprint, "file": f["path"], "action": "revoke in Artifactory (Access > Tokens), then rotate the CI secret"}}]


def fix_iac(repo: pathlib.Path, f: dict) -> list[dict]:
    path = repo / f["path"].split(":")[0]
    text = path.read_text()
    if "CVE-2026-82329" in f["title"]:
        text = text.replace('artifactory_version = "7.111.19"', 'artifactory_version = "7.111.21"   # CVE-2026-82329 (JFrog advisory JFSA-2026-001); CAB-4471')
        strategy = "pin Artifactory to 7.111.21 (fixed release per vendor advisory)"
    elif "non-expiring" in f["title"]:
        text = text.replace("expires_in_seconds = 0 # 0 = never expires", "expires_in_seconds = 3600 # 1h; CI exchanges GitHub OIDC for a fresh token per run")
        strategy = "default access-token TTL 0 -> 3600s"
    elif "0.0.0.0/0" in f["title"]:
        text = text.replace('ingress_cidrs = ["0.0.0.0/0"]', 'ingress_cidrs = var.build_agent_cidrs   # vendor SaaS build agents only (PLAT-771 revisited)')
        text += '\nvariable "build_agent_cidrs" {\n  type        = list(string)\n  description = "Egress CIDRs published by the vendor build agents"\n}\n'
        strategy = "restrict ingress to the build-agent CIDR list (variable, populated by platform-eng)"
    else:
        text = text.replace("  expires_in  = 0\n", "  expires_in  = 0 # TODO remove: replaced by OIDC exchange in ci-shared-workflows v4\n")
        text = text.replace('  for_each    = toset(var.ci_repos)', '  for_each    = toset(var.legacy_ci_repos) # shrinks to [] as repos move to @v4')
        text = text.replace('variable "ci_repos" {', 'variable "legacy_ci_repos" {')
        strategy = "mark static CI tokens legacy; list shrinks as repos move to OIDC (@v4)"
    path.write_text(text)
    tf = shutil.which("terraform") or str(pathlib.Path.home() / ".local/bin/terraform")
    if pathlib.Path(tf).exists():
        run([tf, "fmt", str(path)], repo)
    ok, summary, _ = test_repo(repo)
    return [{"strategy": strategy, "changes": [f["path"].split(":")[0]], "tests_ok": ok, "test_summary": summary}]


def fix_sast(repo: pathlib.Path, f: dict) -> list[dict]:
    path = repo / f["path"].split(":")[0]
    text = path.read_text().replace("data = yaml.load(fh, Loader=yaml.Loader)", "data = yaml.safe_load(fh)")
    path.write_text(text)
    test = repo / "tests/test_limits.py"
    test.write_text(test.read_text() + textwrap.dedent("""

        def test_limits_loader_rejects_python_tags(tmp_path):
            import pytest
            import yaml

            hostile = tmp_path / "limits.yaml"
            hostile.write_text("limits: !!python/object/apply:os.system ['echo pwned']\\n")
            with pytest.raises(yaml.constructor.ConstructorError):
                load_limits(str(hostile))
        """))
    pyproject = repo / "pyproject.toml"
    pyproject.write_text(pyproject.read_text().replace('"PyYAML==5.4.1"', '"PyYAML==6.0.2"'))
    ok, summary, _ = test_repo(repo)
    return [{"strategy": "yaml.load(Loader=yaml.Loader) -> yaml.safe_load; regression test; PyYAML 5.4.1 -> 6.0.2",
             "changes": ["risk_lib/limits.py", "tests/test_limits.py", "pyproject.toml"], "tests_ok": ok, "test_summary": summary}]


def pick_fixer(f: dict):
    if f["kind"] == "sca" and (f["package"] or "").startswith("org.yaml"):
        return fix_sca_core_dates
    if f["kind"] == "sca" and "commons-compress" in (f["package"] or ""):
        return fix_sca_commons_compress
    return {"secret": fix_secret, "iac": fix_iac, "sast": fix_sast}[f["kind"]]


def pr_body(f: dict, attempts: list[dict], branch: str) -> str:
    final = attempts[-1]
    status = "fixed" if final["tests_ok"] and len(attempts) == 1 else ("fixed-with-deviation" if final["tests_ok"] else "blocked")
    lines = [f"# security: {f['scanner_id']} — {f['title']}", "",
             f"**Finding:** `{f['id']}` · severity **{f['severity']}** · {', '.join(f['cve']) or 'no CVE'} · {', '.join(f['cwe'])}",
             f"**Incident:** SEC-2026-0912 (CVE-2026-82329 Artifactory response) · **Branch:** `{branch}` · **Status:** `{status}`",
             target_line(f["repo"]), "",
             "## What changed", *[f"- `{c}`" for c in final["changes"]], "", "## Attempts"]
    for i, a in enumerate(attempts, 1):
        lines.append(f"{i}. {a['strategy']} → {'PASS' if a['tests_ok'] else 'FAIL'} — `{a['test_summary']}`")
        if a.get("reason"):
            lines.append(f"   - why: {a['reason']}")
    lines += ["", "## Test evidence", f"`{final['test_summary']}`", ""]
    if final.get("revocation_required"):
        r = final["revocation_required"]
        lines += ["## Token revocation required", f"- fingerprint `{r['fingerprint']}` in `{r['file']}` — {r['action']}", ""]
    lines += ["## Residual risk",
              "- Fix is in a PR; exposure ends when CODEOWNERS merge and the deploy pipeline runs." if status != "blocked" else "- Not fixed; needs human decision.",
              "- Scanner re-run is scheduled post-merge; this PR does not itself close the finding.", "",
              f"CODEOWNERS reviewers: {', '.join(f.get('owners', [])) or 'see repo CODEOWNERS'}", "", "Devin-Org: engineering"]
    return "\n".join(lines) + "\n"


def remediate(f: dict) -> dict:
    repo = WORKSPACE / f["repo"]
    slug = slug_of(f)
    branch = f"security/{slug}"[:80]
    git(repo, "checkout", "-q", "main")
    git(repo, "branch", "-D", branch)
    git(repo, "checkout", "-q", "-b", branch)
    started = now_iso()
    attempts = pick_fixer(f)(repo, f)
    git(repo, "add", "-A")
    git(repo, "commit", "-q", "-m", f"bug: security {f['scanner_id']} — {f['title'][:60]}")
    patch = git(repo, "format-patch", "-1", "--stdout").stdout
    pr_dir = OUT / "scanner/prs" / slug
    pr_dir.mkdir(parents=True, exist_ok=True)
    (pr_dir / "change.patch").write_text(patch)
    (pr_dir / "PR.md").write_text(pr_body(f, attempts, branch))
    git(repo, "checkout", "-q", "main")
    final = attempts[-1]
    status = "fixed" if final["tests_ok"] and len(attempts) == 1 else ("fixed-with-deviation" if final["tests_ok"] else "blocked")
    result = {"finding_id": f["id"], "repo": f["repo"], "severity": f["severity"], "kind": f["kind"], "cve": f["cve"], "branch": branch,
              "status": status, "attempts": attempts, "started_at": started, "finished_at": now_iso(), "pr_dir": str(pr_dir),
              "owners": f.get("owners", [])}
    write_json(OUT / "scanner/results" / f"{slug}.json", result)
    log(f"{f['repo']:32s} {f['scanner_id']:38s} {status:22s} {final['test_summary']}")
    return result


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--plan", default=str(OUT / "scanner/fanout-plan.json"))
    ap.add_argument("--findings", default=str(OUT / "scanner/findings.json"))
    ap.add_argument("--only", help="substring filter on finding id")
    a = ap.parse_args()
    plan = read_json(pathlib.Path(a.plan))
    findings = {f["id"]: f for f in read_json(pathlib.Path(a.findings))}
    results = []
    for child in plan["children"]:
        f = findings[child["finding_id"]]
        f["owners"] = child["owners"]
        if a.only and a.only not in f["id"]:
            continue
        results.append(remediate(f))
    write_json(OUT / "scanner/results.json", results)
    by = {s: sum(r["status"] == s for r in results) for s in ("fixed", "fixed-with-deviation", "blocked")}
    log(f"done: {by}")


if __name__ == "__main__":
    main()
