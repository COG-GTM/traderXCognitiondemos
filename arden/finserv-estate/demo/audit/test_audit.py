import os
import json
import subprocess
import sys
import urllib.error
from datetime import datetime, timezone
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import ccc_map  # noqa: E402
import trail  # noqa: E402

BEFORE_TF = '''module "artifactory" {
  # Public ingress kept for the vendor SaaS build agents (ticket PLAT-771, 2023).
  ingress_cidrs = ["0.0.0.0/0"]
  access_token_defaults = {
    expires_in_seconds = 0 # 0 = never expires
  }
}
'''
CHAIN = json.loads((HERE / "fixtures/trace/chain.json").read_text())


def _estate(tmp, tf_text, url="https://artifactory.ardencm.internal/x"):
    tf = tmp / "platform-terraform"; (tf / "artifactory").mkdir(parents=True)
    (tf / "artifactory/main.tf").write_text(tf_text)
    wf = tmp / "ci-shared-workflows"; (wf / ".github/workflows").mkdir(parents=True)
    (wf / ".github/workflows/java-build.yml").write_text(f"run: curl {url}\n")
    return tf, wf


def _routes(findings):
    return {f["control"]: f["route"] for f in findings}


def test_before_state_routes_one_fix_one_risk(tmp_path):
    r = _routes(ccc_map.evaluate(*_estate(tmp_path, BEFORE_TF), CHAIN))
    assert r == {"CCC.Core.CN13": "FIX", "CCC.Core.CN05": "RISK", "CCC.Core.CN01": "PASS",
                 "CCC.Core.CN04": "GAP", "CCC.Core.CN09": "GAP"}


def test_after_fix_cn13_passes_and_cn05_stays_with_risk(tmp_path):
    after = BEFORE_TF.replace("expires_in_seconds = 0 # 0 = never expires", "expires_in_seconds = 3600")
    r = _routes(ccc_map.evaluate(*_estate(tmp_path, after), CHAIN))
    assert r["CCC.Core.CN13"] == "PASS" and r["CCC.Core.CN05"] == "RISK"


def test_open_ingress_without_recorded_exception_is_a_fix_not_risk(tmp_path):
    no_exc = BEFORE_TF.replace("  # Public ingress kept for the vendor SaaS build agents (ticket PLAT-771, 2023).\n", "")
    assert _routes(ccc_map.evaluate(*_estate(tmp_path, no_exc), CHAIN))["CCC.Core.CN05"] == "FIX"


def test_plain_http_fails_cn01(tmp_path):
    r = _routes(ccc_map.evaluate(*_estate(tmp_path, BEFORE_TF, "http://artifactory.ardencm.internal/x"), CHAIN))
    assert r["CCC.Core.CN01"] == "FIX"


def test_only_stable_catalog_controls_are_named():
    cat = yaml.safe_load((HERE / "fixtures/ccc-core-v2025.10.yaml").read_text())
    assert cat["version"] == "v2025.10" and cat["status"] == "stable"
    assert all(cid.startswith("CCC.Core.CN") for cid in cat["controls"])


def test_trace_marks_missing_audit_record_instead_of_backfilling():
    ticket = json.loads((HERE / "fixtures/trace/PLAT-1142.json").read_text())
    rows = dict((q, v) for q, v, _ in trail.build(CHAIN, ticket, {"items": []}))
    assert rows["Who asked Devin"] == "MISSING"


def test_trace_links_session_from_audit_log():
    ticket = json.loads((HERE / "fixtures/trace/PLAT-1142.json").read_text())
    log = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    rows = dict((q, v) for q, v, _ in trail.build(CHAIN, ticket, log))
    assert rows["Who asked Devin"].startswith("requester@ardencm.example")
    assert rows["Approved by"].startswith("not approved yet")


def test_fixture_matches_documented_audit_log_schema():
    log = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    assert {"items", "end_cursor", "has_next_page", "total"} <= log.keys()
    for e in log["items"]:
        assert {"audit_log_id", "action", "created_at", "data", "org_id", "user_id", "user_email"} <= e.keys()


def test_missing_expiry_is_gap_not_pass(tmp_path):
    no_expiry = BEFORE_TF.replace("    expires_in_seconds = 0 # 0 = never expires\n", "    refreshable = true\n")
    assert _routes(ccc_map.evaluate(*_estate(tmp_path, no_expiry), CHAIN))["CCC.Core.CN13"] == "GAP"


def test_expiry_from_variable_default_is_resolved(tmp_path):
    via_var = BEFORE_TF.replace("expires_in_seconds = 0 # 0 = never expires", "expires_in_seconds = var.ttl")
    via_var += 'variable "ttl" {\n  type    = number\n  default = 21600\n}\n'
    assert _routes(ccc_map.evaluate(*_estate(tmp_path, via_var), CHAIN))["CCC.Core.CN13"] == "PASS"


def test_live_fetch_follows_cursor(monkeypatch):
    pages = iter([{"items": [{"n": 1}], "has_next_page": True, "end_cursor": "c1"},
                  {"items": [{"n": 2}], "has_next_page": False, "end_cursor": None}])
    seen = []

    class R:
        def __init__(self, req): seen.append(req.full_url); self.p = next(pages)
        def __enter__(self): return self
        def __exit__(self, *a): return False
        def read(self): return json.dumps(self.p).encode()

    monkeypatch.setattr(trail.urllib.request, "urlopen", lambda req, timeout: R(req))
    log = trail.fetch_live("org_x", "t", 1791848340)
    assert [i["n"] for i in log["items"]] == [1, 2] and "after=c1" in seen[1]


def test_pr_rows_are_labelled_as_snapshot():
    ticket = json.loads((HERE / "fixtures/trace/PLAT-1142.json").read_text())
    rows = {q: r for q, _, r in trail.build(CHAIN, ticket, {"items": []})}
    assert all(rows[q].startswith("PR snapshot") for q in ("CI", "Approved by", "Deployed"))


def test_findings_include_evidence_gap_owners_and_pass_actions(tmp_path):
    findings = ccc_map.evaluate(*_estate(tmp_path, BEFORE_TF), CHAIN)
    assert all(f["evidence"] for f in findings)
    assert all("url" in evidence for f in findings for evidence in f["evidence"])
    assert all(f.get("owner") and f.get("due") for f in findings if f["route"] == "GAP")
    assert all(f["action"] == f"No action, monitored by {f['control']}"
               for f in findings if f["route"] == "PASS")


def test_token_instances_count_for_each_and_default_block(tmp_path):
    terraform = '''resource "artifactory_scoped_token" "ci" {
  for_each   = toset(var.ci_repos)
  expires_in = 0
}

variable "ci_repos" {
  type = list(string)
  default = [
    "repo-a",
    "repo-b",
    "repo-c",
  ]
}

access_token_defaults = {
  expires_in_seconds = 0
}
'''
    findings = ccc_map.evaluate(*_estate(tmp_path, terraform), CHAIN)
    cn13 = next(f for f in findings if f["control"] == "CCC.Core.CN13")
    assert "4 tokens" in cn13["finding"]


def test_cn04_passes_when_cloudtrail_configuration_exists(tmp_path):
    terraform = BEFORE_TF + '\nresource "aws_cloudtrail" "audit" {}\n'
    findings = ccc_map.evaluate(*_estate(tmp_path, terraform), CHAIN)
    cn04 = next(f for f in findings if f["control"] == "CCC.Core.CN04")
    assert cn04["route"] == "PASS"
    assert any("aws_cloudtrail" in item["text"] for item in cn04["evidence"])


def test_github_permalink_builds_full_source_line_url(tmp_path):
    repository = tmp_path / "repo"
    terraform = repository / "arden" / "platform-terraform"
    terraform.mkdir(parents=True)
    (terraform / "main.tf").write_text("resource {}\n")
    subprocess.run(["git", "-C", str(repository), "init", "-q"], check=True)
    subprocess.run(["git", "-C", str(repository), "remote", "add", "origin", "git@github.com:Acme/x.git"], check=True)
    subprocess.run(["git", "-C", str(repository), "add", "."], check=True)
    subprocess.run(["git", "-C", str(repository), "-c", "user.name=Test", "-c", "user.email=test@example.com",
                    "commit", "-q", "-m", "fixture"], check=True)
    permalink = ccc_map._github_permalink(terraform)
    assert permalink is not None
    url = ccc_map._evidence_url(terraform, {
        "file": "platform-terraform/main.tf", "line": 1, "text": "resource {}",
    }, permalink)
    assert url == f"https://github.com/Acme/x/blob/{permalink[1]}/arden/platform-terraform/main.tf#L1"


def test_live_missing_org_falls_back_to_recorded_snapshot(tmp_path):
    env = os.environ.copy()
    for key in ("DEVIN_ORG_ID", "DEVIN_AUDIT_TOKEN", "DEVIN_API_KEY"):
        env.pop(key, None)
    result = subprocess.run(
        [sys.executable, str(HERE / "trail.py"), "--live", "--out", str(tmp_path)],
        capture_output=True, text=True, env=env, check=False,
    )
    assert result.returncode == 0
    assert result.stdout.count("Live audit log unavailable: DEVIN_ORG_ID is not set. Showing recorded snapshot.") == 1
    assert "Recorded audit snapshot (fixture)" in result.stdout
    assert "Traceback" not in result.stderr


def test_live_http_403_falls_back_without_traceback(tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("DEVIN_ORG_ID", "org_x")
    monkeypatch.setenv("DEVIN_AUDIT_TOKEN", "bogus")
    monkeypatch.setattr(trail.urllib.request, "urlopen", lambda *args, **kwargs: (_ for _ in ()).throw(
        urllib.error.HTTPError("https://api.devin.ai", 403, "Forbidden", None, None)))
    monkeypatch.setattr(sys, "argv", ["trail.py", "--live", "--out", str(tmp_path)])
    trail.main()
    output = capsys.readouterr().out
    assert output.count("Live audit log unavailable: HTTP 403 Forbidden (the token needs ViewAccountAuditLogs). Showing recorded snapshot.") == 1
    assert "Recorded audit snapshot (fixture)" in output
    assert "Traceback" not in output


def test_risk_memo_and_ci_record_use_safe_current_links(tmp_path):
    tf, wf = _estate(tmp_path, BEFORE_TF)
    findings = ccc_map.evaluate(tf, wf, CHAIN)
    catalog = yaml.safe_load((HERE / "fixtures/ccc-core-v2025.10.yaml").read_text())
    calendar = yaml.safe_load((HERE / "fixtures/audit-calendar.yaml").read_text())
    out = tmp_path / "out"
    ccc_map.write(out, findings, catalog, calendar, CHAIN, tf)
    memo = (out / "evidence-pack/risk-decision-CN05.md").read_text()
    assert "FAIL" not in memo
    ticket = json.loads((HERE / "fixtures/trace/PLAT-1142.json").read_text())
    rows = {question: record for question, _, record in trail.build(CHAIN, ticket, {"items": []})}
    assert "/commit/5c73ebc" in rows["CI"]
    assert "/checks" not in rows["CI"]


def test_ticket_precedes_session_creation():
    ticket = json.loads((HERE / "fixtures/trace/PLAT-1142.json").read_text())
    log = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    ticket_created = datetime.strptime(ticket["created"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc).timestamp()
    session_created = next(item["created_at"] for item in log["items"] if item["action"] == "create_session")
    assert ticket_created < session_created


def test_cn13_gap_has_owner_and_cli_summary_runs(tmp_path):
    no_expiry = BEFORE_TF.replace("    expires_in_seconds = 0 # 0 = never expires\n", "    refreshable = true\n")
    tf, wf = _estate(tmp_path, no_expiry)
    cn13 = next(f for f in ccc_map.evaluate(tf, wf, CHAIN) if f["control"] == "CCC.Core.CN13")
    assert cn13["route"] == "GAP" and cn13["owner"] and cn13["due"]
    import subprocess, sys
    r = subprocess.run([sys.executable, str(HERE / "ccc_map.py"), "--terraform", str(tf), "--workflows", str(wf),
                        "--out", str(tmp_path / "out")], capture_output=True, text=True)
    assert r.returncode == 0, r.stderr


def test_disabled_or_mentioned_logging_does_not_pass(tmp_path):
    tf_text = BEFORE_TF + '\n# aws_cloudtrail is not configured\nresource "aws_s3_bucket" "logs" {\n  enable_log_file_validation = false\n}\n'
    routes = _routes(ccc_map.evaluate(*_estate(tmp_path, tf_text), CHAIN))
    assert routes["CCC.Core.CN04"] == "GAP" and routes["CCC.Core.CN09"] == "GAP"


def test_evidence_pack_readme_links_resolve(tmp_path):
    import re
    tf, wf = _estate(tmp_path, BEFORE_TF)
    catalog = __import__("yaml").safe_load((HERE / "fixtures/ccc-core-v2025.10.yaml").read_text())
    calendar = __import__("yaml").safe_load((HERE / "fixtures/audit-calendar.yaml").read_text())
    out = tmp_path / "out"
    ccc_map.write(out, ccc_map.evaluate(tf, wf, CHAIN), catalog, calendar, CHAIN, tf)
    pack = out / "evidence-pack"
    links = [l for l in re.findall(r"\]\(([^)]+)\)", (pack / "README.md").read_text()) if "risk-decision" in l]
    assert links and all((pack / l).exists() for l in links)


def test_log_validation_resolves_variable_defaults(tmp_path):
    def cn09(default):
        tf_text = BEFORE_TF + ('\nresource "aws_cloudtrail" "audit" {\n  enable_log_file_validation = var.validation_enabled\n}\n'
                               f'variable "validation_enabled" {{\n  type    = bool\n  default = {default}\n}}\n')
        return _routes(ccc_map.evaluate(*_estate(tmp_path / default, tf_text), CHAIN))["CCC.Core.CN09"]
    assert cn09("true") == "PASS" and cn09("false") == "GAP"


def test_cloudtrail_module_stays_gap_but_is_named(tmp_path):
    tf_text = BEFORE_TF + '\nmodule "cloudtrail" {\n  source = "terraform-aws-modules/cloudtrail/aws"\n}\n'
    cn04 = next(f for f in ccc_map.evaluate(*_estate(tmp_path, tf_text), CHAIN) if f["control"] == "CCC.Core.CN04")
    assert cn04["route"] == "GAP" and cn04["owner"] and "CloudTrail module" in cn04["evidence"][0]["text"]


def test_log_validation_expression_is_not_resolved_as_true(tmp_path):
    tf_text = BEFORE_TF + ('\nresource "aws_cloudtrail" "audit" {\n  enable_log_file_validation = var.validation_enabled && false\n}\n'
                           'variable "validation_enabled" {\n  type    = bool\n  default = true\n}\n')
    assert _routes(ccc_map.evaluate(*_estate(tmp_path, tf_text), CHAIN))["CCC.Core.CN09"] == "GAP"
