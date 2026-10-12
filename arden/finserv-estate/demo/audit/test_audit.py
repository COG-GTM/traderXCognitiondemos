import json
import subprocess
import sys
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
