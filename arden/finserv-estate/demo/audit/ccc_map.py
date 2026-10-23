"""Map Arden platform Terraform + shared CI to the stable FINOS CCC core catalog and write an auditor evidence pack.

    python3 demo/audit/ccc_map.py --terraform <platform-terraform dir> --workflows <ci-shared-workflows dir> [--out demo/out/audit]

Every control named here comes from CCC.Core v2025.10 (stable). Each finding ends in exactly one route:
PASS, FIX (Devin opens a PR), RISK (a policy decision; Devin writes a memo and changes no code), or GAP (evidence lives
outside these repos; ask the owner).
"""
from __future__ import annotations

import argparse
import html
import json
import os
import re
import subprocess
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
FIX, RISK, PASS, GAP = "FIX", "RISK", "PASS", "GAP"


def _hits(root: Path, glob: str, pattern: str) -> list[dict]:
    rx = re.compile(pattern)
    out = []
    for f in sorted(root.glob(glob)):
        for n, line in enumerate(f.read_text().splitlines(), 1):
            if rx.search(line):
                out.append({"file": str(f.relative_to(root.parent)), "line": n, "text": line.strip()})
    return out


def _github_permalink(root: Path) -> tuple[str, str] | None:
    try:
        top = Path(subprocess.run(
            ["git", "-C", str(root), "rev-parse", "--show-toplevel"],
            check=True, capture_output=True, text=True,
        ).stdout.strip())
        sha = subprocess.run(
            ["git", "-C", str(root), "rev-parse", "HEAD"],
            check=True, capture_output=True, text=True,
        ).stdout.strip()
        remote = subprocess.run(
            ["git", "-C", str(root), "remote", "get-url", "origin"],
            check=True, capture_output=True, text=True,
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None
    match = re.fullmatch(r"(?:https://github\.com/|git@github\.com:)([^/]+/[^/]+?)(?:\.git)?", remote)
    if not match:
        match = re.fullmatch(r"https://[^/]+/proxy/github\.com/([^/]+/[^/]+?)(?:\.git)?", remote)
    if not match or not sha:
        return None
    return f"https://github.com/{match.group(1)}", sha


def _evidence_url(root: Path, evidence: dict, permalink: tuple[str, str] | None) -> str | None:
    if "url" in evidence and evidence["url"]:
        return evidence["url"]
    if not permalink or evidence.get("line") is None or not evidence.get("file"):
        return None
    base, sha = permalink
    repo_root = Path(subprocess.run(
        ["git", "-C", str(root), "rev-parse", "--show-toplevel"],
        check=True, capture_output=True, text=True,
    ).stdout.strip())
    source_path = (root.parent / evidence["file"]).resolve()
    try:
        relative = source_path.relative_to(repo_root).as_posix()
    except ValueError:
        return None
    return f"{base}/blob/{sha}/{relative}#L{evidence['line']}"


def _tree_url(root: Path, repo_name: str) -> str | None:
    permalink = _github_permalink(root)
    if not permalink:
        return None
    base, sha = permalink
    top = Path(subprocess.run(
        ["git", "-C", str(root), "rev-parse", "--show-toplevel"],
        check=True, capture_output=True, text=True,
    ).stdout.strip())
    try:
        relative = root.resolve().relative_to(top).as_posix()
    except ValueError:
        return None
    return f"{base}/tree/{sha}/{relative}"


def _evidence_label(evidence: dict) -> str:
    if evidence.get("line") is not None:
        return f"{evidence['file']}:{evidence['line']}"
    return evidence.get("text") or evidence["file"]


def _markdown_evidence(evidence: list[dict]) -> str:
    links = []
    for item in evidence:
        label = _evidence_label(item)
        links.append(f"[{label}]({item['url']})" if item.get("url") else label)
    return "<br>".join(links) if links else "none in repo"


def _html_evidence(evidence: list[dict]) -> str:
    links = []
    for item in evidence:
        label = html.escape(_evidence_label(item))
        links.append(f"<a href='{html.escape(item['url'], quote=True)}'>{label}</a>" if item.get("url") else label)
    return "<br>".join(links) if links else "none in repo"


def _markdown_action(text: str, base: str = "evidence-pack/") -> str:
    text = re.sub(r"(https?://[^\s)]+)", lambda m: f"[{m.group(1)}]({m.group(1)})", text)
    return text.replace("risk-decision-CN05.html", f"[risk-decision-CN05.html]({base}risk-decision-CN05.html)")


def _html_action(text: str) -> str:
    escaped = html.escape(text)
    escaped = re.sub(r"(https?://[^\s&lt;]+)",
                     lambda m: f'<a href="{m.group(1)}">{m.group(1)}</a>', escaped)
    return escaped.replace("risk-decision-CN05.html",
                           '<a href="evidence-pack/risk-decision-CN05.html">risk-decision-CN05.html</a>')


def _comment_above(root: Path, hit: dict) -> str:
    lines = (root.parent / hit["file"]).read_text().splitlines()
    i = hit["line"] - 2
    return lines[i].strip().lstrip("#").strip() if i >= 0 and lines[i].strip().startswith("#") else ""


TOKEN_BLOCK = re.compile(r'resource\s+"artifactory_scoped_token"|\baccess_token_defaults\s*=')
EXPIRY = re.compile(r'\bexpires_in(?:_seconds)?\s*=\s*([^\s#]+)')


def _var_default(tf: Path, name: str) -> int | None:
    for f in tf.glob("**/*.tf"):
        m = re.search(r'variable\s+"%s"\s*{(.*?)\n}' % re.escape(name), f.read_text(), re.S)
        if not m:
            continue
        contents = m.group(1)
        d = re.search(r'\bdefault\s*=\s*(\d+)', contents)
        if d:
            return int(d.group(1))
        default = re.search(r'\bdefault\s*=\s*\[(.*?)\]', contents, re.S)
        if default:
            return len(re.findall(r'"(?:\\.|[^"\\])*"', default.group(1)))
    return None


def _block(lines: list[str], start: int) -> list[str]:
    depth, block = 0, []
    for line in lines[start:]:
        block.append(line)
        depth += line.count("{") - line.count("}")
        if depth <= 0 and len(block) > 1:
            break
    return block


def _instances(tf: Path, block: list[str]) -> int | None:
    for_each = next((line for line in block if re.search(r'\bfor_each\s*=', line)), None)
    if for_each is None:
        return 1
    match = re.search(r'\bfor_each\s*=\s*(?:toset\(\s*)?var\.([A-Za-z_]\w*)\s*\)?', for_each)
    if not match:
        return None
    return _var_default(tf, match.group(1))


def token_lifetimes(tf: Path) -> list[dict]:
    """Every token definition with its effective expiry in seconds (None when missing or unresolvable)."""
    out = []
    for f in sorted(tf.glob("**/*.tf")):
        lines = f.read_text().splitlines()
        for i, line in enumerate(lines):
            if not TOKEN_BLOCK.search(line):
                continue
            block = _block(lines, i)
            seconds, at = None, i
            for offset, source_line in enumerate(block):
                m = EXPIRY.search(source_line)
                if m:
                    v, at = m.group(1), i + offset
                    seconds = int(v) if v.isdigit() else _var_default(tf, v[4:]) if v.startswith("var.") else None
            out.append({"file": str(f.relative_to(tf.parent)), "line": at + 1, "text": lines[at].strip(),
                        "seconds": seconds, "instances": _instances(tf, block)})
    return out


def evaluate(tf: Path, wf: Path, chain: dict) -> list[dict]:
    findings = []
    due = next(c["date"] for c in yaml.safe_load((HERE / "fixtures/audit-calendar.yaml").read_text())
               if c.get("kind") == "Arden fixture")

    tokens = token_lifetimes(tf)
    never = [k for k in tokens if k["seconds"] == 0]
    unknown = [k for k in tokens if k["seconds"] is None]
    route = FIX if never else GAP if (unknown or not tokens) else PASS
    total_never = sum(k["instances"] for k in never) if all(k["instances"] is not None for k in never) else None
    breakdown = ", ".join(f"{k['instances'] if k['instances'] is not None else 'unknown'} from {Path(k['file']).name}:{k['line']}"
                          for k in never)
    token_count = f"{total_never} tokens" if total_never is not None else "an unknown number of tokens"
    token_finding = (f"{len(never)} of {len(tokens)} token definitions never expire ({token_count}: {breakdown})" if never
                     else f"{len(unknown)} of {len(tokens)} token definition(s) have no resolvable expiry" if unknown
                     else "no token definitions found" if not tokens
                     else f"all {len(tokens)} token definition(s) expire (max {max(k['seconds'] for k in tokens)}s)")
    token_evidence = [{"file": k["file"], "line": k["line"], "text": k["text"]} for k in (never or unknown or tokens)]
    if not token_evidence:
        token_evidence = [{"file": "platform-terraform tree", "line": None,
                           "text": "searched platform-terraform: no Artifactory token definitions found",
                           "url": _tree_url(tf, "platform-terraform")}]
    if never:
        token_evidence += [
            {"file": f"PR #{chain['pr'].rsplit('/', 1)[-1]}", "line": None, "text": f"PR #{chain['pr'].rsplit('/', 1)[-1]}", "url": chain["pr"]},
            {"file": f"commit {chain['commit']}", "line": None, "text": f"commit {chain['commit']}", "url": chain["commit_url"]},
        ]
    findings.append({
        "control": "CCC.Core.CN13",
        "route": route,
        "evidence": token_evidence,
        "plain": "Credentials should expire quickly so a leaked one stops working on its own.",
        "finding": token_finding,
        "action": f"Devin opened {chain['pr']}" if never else "Ask the owner to set an explicit expiry" if route == GAP else "No action, monitored by CCC.Core.CN13",
    })

    open_ingress = _hits(tf, "**/*.tf", r'ingress_cidrs\s*=\s*\[\s*"0\.0\.0\.0/0"')
    exception = _comment_above(tf, open_ingress[0]) if open_ingress else ""
    risk_evidence = list(open_ingress)
    if exception:
        risk_evidence.append({"file": "evidence-pack/risk-decision-CN05.html", "line": None,
                              "text": "risk-decision-CN05.html", "url": "evidence-pack/risk-decision-CN05.html"})
    elif not open_ingress:
        risk_evidence = [{"file": "platform-terraform tree", "line": None,
                          "text": "searched platform-terraform: no open ingress rule found",
                          "url": _tree_url(tf, "platform-terraform")}]
    findings.append({
        "control": "CCC.Core.CN05",
        "route": (RISK if exception else FIX) if open_ingress else PASS,
        "evidence": risk_evidence,
        "plain": "Only known networks should be able to reach the service.",
        "finding": "Artifactory accepts traffic from any address" if open_ingress else "ingress is restricted",
        "exception": exception,
        "action": ("Routed to risk: the opening is a recorded business exception, so closing it is a policy decision. "
                   "Devin changed no code. See risk-decision-CN05.html") if exception else
                  "Devin opened " + chain["pr"] if open_ingress else "No action, monitored by CCC.Core.CN05",
    })

    urls = _hits(wf, "**/*.yml", r"https?://artifactory") + _hits(wf, "**/*.yaml", r"https?://artifactory")
    plain_http = [h for h in urls if "http://" in h["text"]]
    ci_evidence = plain_http or urls or [{
        "file": "ci-shared-workflows tree", "line": None,
        "text": "searched ci-shared-workflows: no Artifactory URL found",
        "url": _tree_url(wf, "ci-shared-workflows"),
    }]
    findings.append({
        "control": "CCC.Core.CN01",
        "route": FIX if plain_http else PASS,
        "evidence": ci_evidence,
        "plain": "Data in transit is encrypted.",
        "finding": "plain HTTP to Artifactory" if plain_http else f"all {len(urls)} Artifactory call(s) in shared CI use HTTPS",
        "action": f"Devin opened {chain['pr']}" if plain_http else "No action, monitored by CCC.Core.CN01",
    })

    for cid, plain, pattern, description, owner, action in [
        ("CCC.Core.CN04", "Every access and change is logged.", r"aws_cloudtrail|cloudtrail|audit_log",
         "AWS CloudTrail, cloudtrail, or audit_log", "Cloud platform owner (@ardencm/platform-eng)",
         "Confirm which account-level trail (for example AWS CloudTrail) covers the Artifactory and scheduler accounts, and attach the trail config"),
        ("CCC.Core.CN09", "Logs cannot be quietly edited.", r"enable_log_file_validation|object_lock|log_file_validation",
         "enable_log_file_validation, object_lock, or log_file_validation", "Security engineering (@ardencm/security-eng)",
         "Show where those logs are stored and what makes them tamper-evident (for example CloudTrail log file validation or S3 Object Lock)"),
    ]:
        hits = _hits(tf, "**/*.tf", pattern) + _hits(wf, "**/*.yml", pattern) + _hits(wf, "**/*.yaml", pattern)
        if hits:
            findings.append({"control": cid, "route": PASS, "evidence": hits, "plain": plain,
                             "finding": f"logging configuration found in {len(hits)} location(s)",
                             "action": f"No action, monitored by {cid}"})
        else:
            note = f"searched platform-terraform, ci-shared-workflows: no {description} found"
            findings.append({
                "control": cid, "route": GAP,
                "evidence": [
                    {"file": "platform-terraform tree", "line": None, "text": note,
                     "url": _tree_url(tf, "platform-terraform")},
                    {"file": "ci-shared-workflows tree", "line": None, "text": note,
                     "url": _tree_url(wf, "ci-shared-workflows")},
                ],
                "plain": plain, "finding": "not defined in platform-terraform or ci-shared-workflows",
                "owner": owner, "due": due, "due_action": action,
                "action": f"Owner: {owner}. Due {due}: {action}",
            })
    permalink = _github_permalink(tf)
    for finding in findings:
        for evidence in finding["evidence"]:
            evidence["url"] = _evidence_url(tf if evidence["file"].startswith("platform-terraform/") else wf,
                                            evidence, permalink if evidence["file"].startswith("platform-terraform/") else _github_permalink(wf))
    return findings


def write(out: Path, findings: list[dict], catalog: dict, calendar: list, chain: dict, tf: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    pack = out / "evidence-pack"
    pack.mkdir(exist_ok=True)
    for finding in findings:
        finding["title"] = catalog["controls"][finding["control"]]
    (out / "control-map.json").write_text(json.dumps({
        "catalog": f"{catalog['catalog']} {catalog['version']}",
        "catalog_status": catalog["status"], "source": catalog["source"], "findings": findings,
    }, indent=2))

    risk = next(f for f in findings if f["control"] == "CCC.Core.CN05")
    risk_lines = [
        "# Risk decision request: CCC.Core.CN05 Prevent Access from Untrusted Entities", "",
        f"- Evidence: `{risk['evidence'][0]['file']}:{risk['evidence'][0]['line']}` `{risk['evidence'][0]['text']}`",
        f"- Recorded exception: \"{risk['exception']}\"",
        "- Why Devin did not change the code: closing the opening breaks the vendor build agents that the exception exists for. "
        "Whether that exception still stands is a business risk call, not an engineering one.",
        "- Decision needed from risk (pick one): renew the exception with an expiry date; or restrict to the vendor's published "
        "address ranges (platform-eng supplies them); or retire the vendor agents.",
        "- Owner: @ardencm/security-eng. Until decided, the control stays RISK (open, owned by security-eng) in the evidence pack.", "",
    ]
    if risk["route"] == RISK:
        (pack / "risk-decision-CN05.md").write_text("\n".join(risk_lines))
        risk_html = "".join(f"<li>{html.escape(line[2:])}</li>" for line in risk_lines if line.startswith("- "))
        (pack / "risk-decision-CN05.html").write_text(
            "<!doctype html><meta charset=utf-8><title>CN05 risk decision</title>"
            "<style>body{font:15px system-ui;margin:32px}li{margin:12px 0}</style>"
            "<h2>Risk decision request: CCC.Core.CN05 Prevent Access from Untrusted Entities</h2>"
            f"<ul>{risk_html}</ul>"
        )

    md = [f"# Control map: Arden platform vs {catalog['catalog']} {catalog['version']} ({catalog['status']})", "",
          f"Catalog source: {catalog['source']}",
          "Draft catalogs deliberately not used: " + ", ".join(catalog["draft_catalogs_not_used"]), "",
          "## Audit calendar", ""]
    md += [f"- {c['date']}: {c['name']} ({c['kind']}; {c['source']})" for c in calendar]
    md += ["", "## Findings", "", "| Control | What it means | Route | Finding | Evidence | Action |", "|---|---|---|---|---|---|"]
    for finding in findings:
        evidence = _markdown_evidence(finding["evidence"])
        action = _markdown_action(finding["action"])
        md.append(f"| {finding['control']} {finding['title']} | {finding['plain']} | **{finding['route']}** | "
                  f"{finding['finding']} | {evidence} | {action} |")
    (out / "control-map.md").write_text("\n".join(md) + "\n")

    rows = []
    for finding in findings:
        action = _markdown_action(finding["action"], base="")
        evidence = _markdown_evidence(finding["evidence"])
        rows.append(f"- {finding['control']} {finding['title']}: **{finding['route']}**. {finding['finding']} "
                    f"Evidence: {evidence}. {action}")
    (pack / "README.md").write_text("\n".join([
        "# Auditor evidence pack: Arden platform controls", "",
        f"Catalog: {catalog['catalog']} {catalog['version']} ({catalog['status']}), {catalog['source']}", "",
        *rows, "",
        "## Change trail for the fix", "",
        f"- Ticket {chain['ticket']}, Devin session {chain['devin_session']}, PR {chain['pr']} "
        f"(commit {chain['commit_url']}).",
        "- Full who/why/approved trail: ../traceability.md (generated by demo/audit/trail.py).", ""]))

    colour = {PASS: "#1b7f3b", FIX: "#b26a00", RISK: "#b00020", GAP: "#555"}
    trs = "".join(
        f"<tr><td><b>{html.escape(finding['control'])}</b><br>{html.escape(finding['title'])}</td>"
        f"<td>{html.escape(finding['plain'])}</td>"
        f"<td style='color:#fff;background:{colour[finding['route']]};text-align:center'><b>{finding['route']}</b></td>"
        f"<td>{html.escape(finding['finding'])}</td><td>{_html_evidence(finding['evidence'])}</td>"
        f"<td>{_html_action(finding['action'])}</td></tr>" for finding in findings
    )
    permalink = _github_permalink(tf)
    if permalink:
        base, sha = permalink
        scanned = f"{base}/commit/{sha}"
        scanned_line = (f"Scanned: <a href='{scanned}'>COG-GTM/Springboot-BankApp @ {sha[:7]}</a> "
                        "(arden/platform-terraform, arden/ci-shared-workflows)")
    else:
        scanned_line = "Scanned: local source tree (arden/platform-terraform, arden/ci-shared-workflows)"
    counts = {route: sum(f["route"] == route for f in findings) for route in (FIX, RISK, PASS, GAP)}
    chips = " · ".join(
        f"<span style='display:inline-block;border:1px solid #bbb;border-radius:14px;padding:5px 10px;margin-right:6px'>"
        f"{counts[route]} {route}</span>" for route in (FIX, RISK, PASS, GAP)
    )
    audit_fixture = json.loads((HERE / "fixtures/trace/devin-audit-log.json").read_text())
    if os.environ.get("FS_AUDIT_LIVE") == "1":
        trail_line = "Change trail: see traceability.html for live audit-log status"
    else:
        trail_line = f"Recorded audit snapshot (fixture), captured {audit_fixture['_captured']}"
    source_link = f"<a href='{html.escape(catalog['source'], quote=True)}'>{html.escape(catalog['source'])}</a>"
    (out / "control-map.html").write_text(
        "<!doctype html><meta charset=utf-8><title>Control map</title>"
        "<style>body{font:15px system-ui;margin:32px}table{border-collapse:collapse;width:100%;table-layout:fixed}"
        "td,th{border:1px solid #ccc;padding:8px;vertical-align:top;word-break:break-word;overflow-wrap:anywhere}"
        "th{background:#f3f3f3;text-align:left}a{overflow-wrap:anywhere}</style>"
        f"<h2>Arden platform vs FINOS {catalog['catalog']} {catalog['version']} (stable)</h2>"
        f"<p>Catalog source: {source_link}</p><p>{scanned_line}</p><p>{chips}</p>"
        "<p>Every row links to its evidence: a file line, a PR, a memo, or a named owner question.</p>"
        f"<p><a href='traceability.html'>Change trail for the CN13 fix (traceability.html)</a>: "
        f"{html.escape(trail_line)}</p>"
        f"<table><tr><th>Control</th><th>What it means</th><th>Route</th><th>Finding</th>"
        f"<th>Evidence</th><th>Action</th></tr>{trs}</table>"
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--terraform", type=Path, required=True)
    ap.add_argument("--workflows", type=Path, required=True)
    ap.add_argument("--out", type=Path, default=HERE.parent / "out" / "audit")
    a = ap.parse_args()
    catalog = yaml.safe_load((HERE / "fixtures/ccc-core-v2025.10.yaml").read_text())
    calendar = yaml.safe_load((HERE / "fixtures/audit-calendar.yaml").read_text())
    chain = json.loads((HERE / "fixtures/trace/chain.json").read_text())
    findings = evaluate(a.terraform.resolve(), a.workflows.resolve(), chain)
    write(a.out, findings, catalog, calendar, chain, a.terraform.resolve())
    for f in findings:
        print(f"  {f['route']:<4} {f['control']} {catalog['controls'][f['control']]}: {f['finding']}")
        if f["route"] == GAP:
            print(f"       owner: {f['owner']}, due {f['due']}")


if __name__ == "__main__":
    main()
