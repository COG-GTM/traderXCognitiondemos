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
import re
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


def _comment_above(root: Path, hit: dict) -> str:
    lines = (root.parent / hit["file"]).read_text().splitlines()
    i = hit["line"] - 2
    return lines[i].strip().lstrip("#").strip() if i >= 0 and lines[i].strip().startswith("#") else ""


def evaluate(tf: Path, wf: Path, chain: dict) -> list[dict]:
    findings = []

    never = _hits(tf, "**/*.tf", r"\bexpires_in(_seconds)?\s*=\s*0\b")
    findings.append({
        "control": "CCC.Core.CN13",
        "route": FIX if never else PASS,
        "evidence": never or _hits(tf, "**/*.tf", r"\bexpires_in(_seconds)?\s*="),
        "plain": "Credentials should expire quickly so a leaked one stops working on its own.",
        "finding": f"{len(never)} token definition(s) never expire" if never else "every token definition has an expiry",
        "action": f"Devin opened {chain['pr']}" if never else "none",
    })

    open_ingress = _hits(tf, "**/*.tf", r'ingress_cidrs\s*=\s*\[\s*"0\.0\.0\.0/0"')
    exception = _comment_above(tf, open_ingress[0]) if open_ingress else ""
    findings.append({
        "control": "CCC.Core.CN05",
        "route": (RISK if exception else FIX) if open_ingress else PASS,
        "evidence": open_ingress,
        "plain": "Only known networks should be able to reach the service.",
        "finding": "Artifactory accepts traffic from any address" if open_ingress else "ingress is restricted",
        "exception": exception,
        "action": ("Routed to risk: the opening is a recorded business exception, so closing it is a policy decision. "
                   "Devin changed no code. See risk-decision-CN05.md") if exception else "none",
    })

    urls = _hits(wf, "**/*.yml", r"https?://artifactory")
    plain_http = [h for h in urls if "http://" in h["text"]]
    findings.append({
        "control": "CCC.Core.CN01",
        "route": FIX if plain_http else PASS,
        "evidence": plain_http or urls,
        "plain": "Data in transit is encrypted.",
        "finding": "plain HTTP to Artifactory" if plain_http else f"all {len(urls)} Artifactory call(s) in shared CI use HTTPS",
        "action": "none",
    })

    for cid, plain, ask in [
        ("CCC.Core.CN04", "Every access and change is logged.", "Which account-level trail (for example AWS CloudTrail) covers the Artifactory and scheduler accounts?"),
        ("CCC.Core.CN09", "Logs cannot be quietly edited.", "Where are those logs stored, and what makes them tamper-evident?"),
    ]:
        findings.append({"control": cid, "route": GAP, "evidence": [], "plain": plain,
                         "finding": "not defined in platform-terraform or ci-shared-workflows", "action": f"Ask the owner: {ask}"})
    return findings


def write(out: Path, findings: list[dict], catalog: dict, calendar: list, chain: dict) -> None:
    out.mkdir(parents=True, exist_ok=True)
    for f in findings:
        f["title"] = catalog["controls"][f["control"]]
    (out / "control-map.json").write_text(json.dumps({"catalog": f"{catalog['catalog']} {catalog['version']}",
                                                      "catalog_status": catalog["status"], "source": catalog["source"],
                                                      "findings": findings}, indent=2))
    md = [f"# Control map: Arden platform vs {catalog['catalog']} {catalog['version']} ({catalog['status']})", "",
          f"Catalog source: {catalog['source']}",
          "Draft catalogs deliberately not used: " + ", ".join(catalog["draft_catalogs_not_used"]), "",
          "## Audit calendar", ""]
    md += [f"- {c['date']}: {c['name']} ({c['kind']}; {c['source']})" for c in calendar]
    md += ["", "## Findings", "", "| Control | What it means | Route | Finding | Evidence | Action |", "|---|---|---|---|---|---|"]
    for f in findings:
        ev = "<br>".join(f"`{e['file']}:{e['line']}`" for e in f["evidence"][:4]) or "none in repo"
        md.append(f"| {f['control']} {f['title']} | {f['plain']} | **{f['route']}** | {f['finding']} | {ev} | {f['action']} |")
    (out / "control-map.md").write_text("\n".join(md) + "\n")

    pack = out / "evidence-pack"
    pack.mkdir(exist_ok=True)
    risk = next(f for f in findings if f["control"] == "CCC.Core.CN05")
    if risk["route"] == RISK:
        (pack / "risk-decision-CN05.md").write_text("\n".join([
            "# Risk decision request: CCC.Core.CN05 Prevent Access from Untrusted Entities", "",
            f"- Evidence: `{risk['evidence'][0]['file']}:{risk['evidence'][0]['line']}` `{risk['evidence'][0]['text']}`",
            f"- Recorded exception: \"{risk['exception']}\"",
            "- Why Devin did not change the code: closing the opening breaks the vendor build agents that the exception exists for. "
            "Whether that exception still stands is a business risk call, not an engineering one.",
            "- Decision needed from risk (pick one): renew the exception with an expiry date; or restrict to the vendor's published "
            "address ranges (platform-eng supplies them); or retire the vendor agents.",
            "- Owner: @ardencm/security-eng. Until decided, the control stays FAIL in the evidence pack.", ""]))
    rows = [f"- {f['control']} {f['title']}: **{f['route']}**. {f['action']}" for f in findings]
    (pack / "README.md").write_text("\n".join([
        "# Auditor evidence pack: Arden platform controls", "",
        f"Catalog: {catalog['catalog']} {catalog['version']} ({catalog['status']}), {catalog['source']}", "",
        *rows, "",
        "## Change trail for the fix", "",
        f"- Ticket {chain['ticket']}, Devin session {chain['devin_session']}, PR {chain['pr']} (commit {chain['commit']}).",
        "- Full who/why/approved trail: ../traceability.md (generated by demo/audit/trail.py).", ""]))

    colour = {PASS: "#1b7f3b", FIX: "#b26a00", RISK: "#b00020", GAP: "#555"}
    trs = "".join(
        f"<tr><td><b>{html.escape(f['control'])}</b><br>{html.escape(f['title'])}</td><td>{html.escape(f['plain'])}</td>"
        f"<td style='color:#fff;background:{colour[f['route']]};text-align:center'><b>{f['route']}</b></td>"
        f"<td>{html.escape(f['finding'])}</td><td>{html.escape(f['action'])}</td></tr>" for f in findings)
    (out / "control-map.html").write_text(
        "<!doctype html><meta charset=utf-8><title>Control map</title><style>body{font:15px system-ui;margin:32px}"
        "table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:8px;vertical-align:top}"
        "th{background:#f3f3f3;text-align:left}</style>"
        f"<h2>Arden platform vs FINOS {catalog['catalog']} {catalog['version']} (stable)</h2>"
        f"<p>Source: <a href='{catalog['source']}'>{catalog['source']}</a></p>"
        f"<table><tr><th>Control</th><th>What it means</th><th>Route</th><th>Finding</th><th>Action</th></tr>{trs}</table>")


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
    write(a.out, findings, catalog, calendar, chain)
    for f in findings:
        print(f"  {f['route']:<4} {f['control']} {catalog['controls'][f['control']]}: {f['finding']}")


if __name__ == "__main__":
    main()
