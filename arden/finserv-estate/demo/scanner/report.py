#!/usr/bin/env python3
"""Demo 1 step 4 — exposure burn-down dashboard + regulator-mapped remediation report.

Reads demo/out/scanner/{findings,fanout-plan,results}.json and writes:
  demo/out/scanner/burndown.html             dark dashboard: exposure by severity over the run, per-repo status, PR list
  demo/reports/demo1-remediation-report.md   leave-behind: SEC Item 1.05 / DORA Art.19 field mapping + evidence per finding
  demo/reports/demo1-burndown.html           copy of the dashboard for the leave-behind bundle

The regulatory sections are a *drafting aid*: each field is populated from run evidence and marked
"[analyst to confirm]" where a human judgement (materiality, classification) is required.
"""
from __future__ import annotations

import datetime as dt
import re
import pathlib
import shutil
import sys
from collections import Counter

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, REPORTS, esc, html_page, log, read_json  # noqa: E402

SEV_ORDER = ["critical", "high", "medium", "low"]
SEV_COLOR = {"critical": "#f87171", "high": "#fb923c", "medium": "#fbbf24", "low": "#818cf8"}
STATUS_PILL = {"fixed": "good", "fixed-with-deviation": "warn", "blocked": "bad", "pending": "info"}


def short_summary(s: str) -> str:

    m = re.match(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)(.*)", s)
    if m:
        n, f, e, _, rest = m.groups()
        return f"{n} tests · {int(f) + int(e)} fail" + (" ·" + rest if rest else "")
    return s


def parse_ts(s: str) -> dt.datetime:
    return dt.datetime.strptime(s, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=dt.timezone.utc)


def burndown_series(findings: list[dict], results: dict[str, dict]) -> tuple[list[dt.datetime], dict[str, list[int]]]:
    """Open findings per severity at each remediation event (finish time). Blocked findings stay open."""
    t0 = min(parse_ts(r["started_at"]) for r in results.values())
    events = sorted(
        (parse_ts(r["finished_at"]), fid) for fid, r in results.items() if r["status"] != "blocked"
    )
    open_by_sev = Counter(f["severity"] for f in findings)
    times = [t0]
    series = {s: [open_by_sev[s]] for s in SEV_ORDER}
    by_id = {f["id"]: f for f in findings}
    for t, fid in events:
        open_by_sev[by_id[fid]["severity"]] -= 1
        times.append(t)
        for s in SEV_ORDER:
            series[s].append(open_by_sev[s])
    return times, series


def svg_burndown(times: list[dt.datetime], series: dict[str, list[int]]) -> str:
    w, h, pad = 900, 260, 40
    total0 = sum(v[0] for v in series.values())
    span = max((times[-1] - times[0]).total_seconds(), 1)
    xs = [pad + (t - times[0]).total_seconds() / span * (w - 2 * pad) for t in times]

    def y(v: int) -> float:
        return h - pad - v / max(total0, 1) * (h - 2 * pad)

    out = [f'<svg viewBox="0 0 {w} {h}" width="100%" role="img" aria-label="exposure burn-down">']
    for g in range(0, total0 + 1, max(total0 // 4, 1)):
        out.append(f'<line x1="{pad}" x2="{w-pad}" y1="{y(g):.1f}" y2="{y(g):.1f}" stroke="rgba(255,255,255,.08)"/>'
                   f'<text x="{pad-8}" y="{y(g)+4:.1f}" text-anchor="end">{g}</text>')
    # stacked area, critical on the bottom
    cum = [0] * len(times)
    for sev in reversed(SEV_ORDER):
        lower = cum[:]
        cum = [c + v for c, v in zip(cum, series[sev])]
        pts = " ".join(f"{x:.1f},{y(v):.1f}" for x, v in zip(xs, cum))
        back = " ".join(f"{x:.1f},{y(v):.1f}" for x, v in zip(reversed(xs), reversed(lower)))
        out.append(f'<polygon points="{pts} {back}" fill="{SEV_COLOR[sev]}" fill-opacity=".55" stroke="{SEV_COLOR[sev]}" stroke-width="1.5"/>')
    out.append(f'<text x="{pad}" y="{h-10}">{times[0].strftime("%H:%M:%S")}Z — parent session start</text>')
    out.append(f'<text x="{w-pad}" y="{h-10}" text-anchor="end">{times[-1].strftime("%H:%M:%S")}Z — last child PR</text>')
    legend_x = w - pad - 4 * 90
    for i, sev in enumerate(SEV_ORDER):
        out.append(f'<rect x="{legend_x + i*90}" y="8" width="10" height="10" fill="{SEV_COLOR[sev]}"/>'
                   f'<text x="{legend_x + i*90 + 14}" y="17">{sev}</text>')
    out.append("</svg>")
    return "".join(out)


def build_dashboard(findings: list[dict], plan: dict, results: dict[str, dict]) -> str:
    times, series = burndown_series(findings, results)
    statuses = Counter(r["status"] for r in results.values())
    sev = Counter(f["severity"] for f in findings)
    elapsed = (times[-1] - times[0]).total_seconds()
    open_now = sum(v[-1] for v in series.values())
    cards = [
        ("findings ingested", len(findings), f"{len({f['repo'] for f in findings})} repos · scanner: {findings[0]['scanner']}"),
        ("child sessions", len(plan["children"]), "one per finding · " + plan["parent"]["mode"]),
        ("PRs ready for review", statuses["fixed"] + statuses["fixed-with-deviation"],
         f"{statuses['fixed-with-deviation']} with documented deviation"),
        ("still exposed", open_now, f"{statuses['blocked']} blocked · human owner paged" if open_now else "all findings have a PR"),
        ("wall clock", f"{int(elapsed//60)}m {int(elapsed%60)}s", "parent start → last PR"),
        ("critical / high", f"{sev['critical']} / {sev['high']}", f"medium {sev['medium']} · low {sev['low']}"),
    ]
    body = [f"<h1>Exposure burn-down — {esc(plan['parent']['incident'])}</h1>",
            "<p class='sub'>Devin is the remediation layer: scanner findings in, reviewable PRs out. Nothing here merges without CODEOWNERS approval.</p>",
            "<div class='grid'>"]
    body += [f"<div class='card'><div class='k'>{esc(k)}</div><div class='v'>{esc(v)}</div><div class='k' style='text-transform:none;letter-spacing:0;margin-top:4px'>{esc(sub)}</div></div>"
             for k, v, sub in cards]
    body.append("</div>")
    body.append("<h2>Open findings by severity over the run</h2><div class='card'>" + svg_burndown(times, series) + "</div>")

    body.append("<h2>Per-finding outcome</h2><table><tr><th>repo</th><th>finding</th><th>sev</th><th>kind</th><th>status</th><th>attempts</th><th>evidence</th><th>reviewers</th></tr>")
    for f in sorted(findings, key=lambda f: (SEV_ORDER.index(f["severity"]), f["repo"])):
        r = results.get(f["id"], {"status": "pending", "attempts": [], "owners": []})
        att = "<br>".join(
            f"{i+1}. {esc(a['strategy'])} → <span class='pill {'good' if a['tests_ok'] else 'bad'}'>{'PASS' if a['tests_ok'] else 'FAIL'}</span>"
            for i, a in enumerate(r["attempts"]))
        ev = esc(short_summary(r["attempts"][-1]["test_summary"])) if r["attempts"] else "—"
        body.append(f"<tr><td class='mono'>{esc(f['repo'])}</td><td class='mono'>{esc(f['scanner_id'])}<br><span style='color:var(--muted)'>{esc(f['title'])}</span></td>"
                    f"<td><span class='pill' style='color:{SEV_COLOR[f['severity']]}'>{f['severity']}</span></td><td>{esc(f['kind'])}</td>"
                    f"<td><span class='pill {STATUS_PILL[r['status']]}'>{esc(r['status'])}</span></td><td>{att}</td><td class='mono'>{ev}</td>"
                    f"<td class='mono'>{esc(' '.join(r.get('owners', [])))}</td></tr>")
    body.append("</table>")

    dev = [r for r in results.values() if r["status"] == "fixed-with-deviation"]
    if dev:
        body.append("<h2>Deviations from the scanner's recommended fix</h2>")
        for r in dev:
            why = next((a.get("why") for a in r["attempts"] if a.get("why")), "")
            body.append(f"<div class='card'><b class='mono'>{esc(r['finding_id'])}</b><p>{esc(why)}</p>"
                        "<p style='color:var(--muted)'>First attempt followed the scanner upgrade path and failed the consumer contract test; "
                        "the child pinned the security-only patch release, added a compensating wire-format test, and recorded both attempts in the PR.</p></div>")
    return html_page("Exposure burn-down", "".join(body),
                     f"Generated {dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%d %H:%M UTC')} from demo/out/scanner/results.json · synthetic estate (Arden Capital Markets) · figures are run evidence, not customer data")


def build_report(findings: list[dict], plan: dict, results: dict[str, dict]) -> str:
    times, series = burndown_series(findings, results)
    statuses = Counter(r["status"] for r in results.values())
    sev = Counter(f["severity"] for f in findings)
    repos = sorted({f["repo"] for f in findings})
    kinds = Counter(f["kind"] for f in findings)
    t_detect = min(parse_ts(f["received_at"]) for f in findings)
    t_start, t_end = times[0], times[-1]
    secrets = [f for f in findings if f["kind"] == "secret"]
    dev = [r for r in results.values() if r["status"] == "fixed-with-deviation"]
    blocked = [r for r in results.values() if r["status"] == "blocked"]

    def ts(t: dt.datetime) -> str:
        return t.strftime("%Y-%m-%d %H:%M:%S UTC")

    L: list[str] = []
    L.append(f"# Remediation report — {plan['parent']['incident']}")
    L.append("")
    L.append("Synthetic estate: Arden Capital Markets (fixture). Generated by `demo/scanner/report.py` from the run artifacts in `demo/out/scanner/`. "
             "Devin acted on scanner output as the remediation layer; it did not scan, detect, or merge.")
    L.append("")
    L.append("## 1. Run summary")
    L.append("")
    L.append("| | |")
    L.append("|---|---|")
    L.append(f"| Scanner input | `{findings[0]['scanner']}` webhook fixture (swappable: `FS_SCANNER_SOURCE=snyk-api` or `sonarqube`) |")
    L.append(f"| Findings ingested | {len(findings)} across {len(repos)} repos — {', '.join(f'{k} {v}' for k, v in kinds.items())} |")
    L.append(f"| Severity | critical {sev['critical']} · high {sev['high']} · medium {sev['medium']} · low {sev['low']} |")
    L.append(f"| Fan-out | 1 parent → {len(plan['children'])} child sessions (one per finding), mode `{plan['parent']['mode']}` |")
    L.append(f"| Outcome | fixed {statuses['fixed']} · fixed-with-deviation {statuses['fixed-with-deviation']} · blocked {statuses['blocked']} |")
    L.append(f"| Wall clock | {ts(t_start)} → {ts(t_end)} ({int((t_end - t_start).total_seconds())} s) |")
    L.append(f"| Still exposed at report time | {sum(v[-1] for v in series.values())} finding(s) |")
    L.append("| Merged / deployed | none — every change is a PR awaiting CODEOWNERS review |")
    L.append("")

    L.append("## 2. SEC Form 8-K Item 1.05 — drafting aid")
    L.append("")
    L.append("Item 1.05 requires disclosure within four business days of a *materiality determination*, covering the material aspects of the nature, "
             "scope and timing of the incident and its material (or reasonably likely material) impact. The fields below are pre-filled from run evidence; "
             "materiality is a human determination and is left open.")
    L.append("")
    L.append("| Item 1.05 element | Evidence from this run | Status |")
    L.append("|---|---|---|")
    L.append(f"| Nature | Third-party disclosure: {plan['parent']['incident']}. Fleet exposure via `org.yaml:snakeyaml` (CVE-2022-1471) through the shared `core-dates` library, "
             f"`commons-compress` (CVE-2024-25710), {len(secrets)} committed Artifactory tokens, {kinds['iac']} Artifactory IaC misconfigurations, {kinds['sast']} unsafe YAML loader | populated |")
    L.append(f"| Scope | {len(repos)} repositories: {', '.join(f'`{r}`' for r in repos)} | populated |")
    L.append(f"| Timing | Scanner findings received {ts(t_detect)}; remediation PRs opened {ts(t_start)}–{ts(t_end)} | populated |")
    L.append("| Material impact / reasonably likely impact | No evidence of exploitation in this run (scanner findings only). Impact assessment requires log review by the SOC | **[analyst to confirm]** |")
    L.append("| Materiality determination date | — | **[analyst to confirm]** (starts the 4-business-day clock) |")
    L.append("| Information not yet determined | Whether any committed token was used outside CI; whether Artifactory ingress from `0.0.0.0/0` was reached | **[analyst to confirm]** |")
    L.append("")

    L.append("## 3. DORA Art. 19 major-incident notification — drafting aid")
    L.append("")
    L.append("Initial notification is due 4 hours after classification as *major* and no later than 24 hours after the entity became aware; "
             "intermediate report within 72 hours; final report within one month. Classification (Art. 18 / RTS criteria) is a human decision.")
    L.append("")
    L.append("| Notification field | Evidence from this run | Status |")
    L.append("|---|---|---|")
    L.append(f"| Incident reference | {plan['parent']['incident']} | populated |")
    L.append(f"| Date/time of detection | {ts(t_detect)} (scanner webhook) | populated |")
    L.append("| Date/time of classification as major | — | **[analyst to confirm]** (starts the 4-hour clock) |")
    L.append("| Description | Vulnerable dependency versions and credential/IaC exposures related to an actively exploited Artifactory auth bypass; no confirmed intrusion | populated |")
    L.append("| Classification criteria met | Critical services affected (settlement instruction, custody gateway); data losses / clients affected: none evidenced | **[analyst to confirm]** |")
    L.append("| Discovery | Automated scanner (Snyk) run against all repositories | populated |")
    L.append("| Origination | Third-party ICT provider software (JFrog Artifactory) and open-source dependencies | populated |")
    L.append(f"| Actions taken / remediation status | {statuses['fixed'] + statuses['fixed-with-deviation']} of {len(findings)} findings have a reviewable PR; {statuses['blocked']} blocked and escalated to owners | populated |")
    L.append("| Business continuity plan activated | No | populated |")
    L.append("| Other financial entities / third parties impacted | `ci-shared-workflows` consumers (all repos using `@v3` static tokens) | populated |")
    L.append("")

    L.append("## 4. Per-finding evidence")
    L.append("")
    L.append("| repo | finding | sev | status | attempts | final test evidence | CODEOWNERS |")
    L.append("|---|---|---|---|---|---|---|")
    for f in sorted(findings, key=lambda f: (SEV_ORDER.index(f["severity"]), f["repo"])):
        r = results.get(f["id"], {"status": "pending", "attempts": [], "owners": []})
        att = "; ".join(f"{a['strategy']} → {'PASS' if a['tests_ok'] else 'FAIL'}" for a in r["attempts"])
        ev = r["attempts"][-1]["test_summary"] if r["attempts"] else "—"
        L.append(f"| `{f['repo']}` | `{f['scanner_id']}` {f['title']} | {f['severity']} | {r['status']} | {att} | `{ev}` | {' '.join(r.get('owners', []))} |")
    L.append("")

    if dev:
        L.append("## 5. Deviations from the scanner's recommended fix")
        L.append("")
        for r in dev:
            why = next((a.get("why") for a in r["attempts"] if a.get("why")), "")
            L.append(f"### `{r['finding_id']}`")
            L.append("")
            for i, a in enumerate(r["attempts"], 1):
                L.append(f"{i}. **{a['strategy']}** → {'PASS' if a['tests_ok'] else 'FAIL'} — `{a['test_summary']}`")
            L.append("")
            L.append(f"Reason: {why}")
            L.append("")
            L.append(f"PR artifact: `{pathlib.Path(r['pr_dir']).relative_to(OUT.parent.parent)}/PR.md`")
            L.append("")
    if blocked:
        L.append("## 6. Blocked findings (human owner required)")
        L.append("")
        for r in blocked:
            L.append(f"- `{r['finding_id']}` — {r['attempts'][-1]['test_summary'] if r['attempts'] else 'no attempt recorded'} → {' '.join(r.get('owners', []))}")
        L.append("")

    L.append("## 7. Residual risk and next steps")
    L.append("")
    L.append("- Exposure ends when CODEOWNERS merge each PR and the deploy pipeline runs; this report does not close any finding.")
    L.append("- Committed tokens were removed from source; **revocation in Artifactory is a separate action** (`platform-terraform` PR rotates the TTL, it does not revoke existing tokens).")
    L.append("- Re-run the scanner post-merge to confirm the findings clear; `FS_SCANNER_SOURCE=snyk-api` pulls live results with the same normalizer.")
    L.append("- `core-dates` 1.5.0 remains unadopted by FIX-emitting services (ADR 0007); the wire-format change needs a coordinated release with `custody-gateway`.")
    L.append("")
    L.append(f"_Generated {dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}. All repository names, tokens and identifiers are synthetic fixtures._")
    return "\n".join(L)


def main() -> None:
    findings = read_json(OUT / "scanner/findings.json")
    plan = read_json(OUT / "scanner/fanout-plan.json")
    results = {r["finding_id"]: r for r in read_json(OUT / "scanner/results.json")}
    REPORTS.mkdir(parents=True, exist_ok=True)
    dash = OUT / "scanner/burndown.html"
    dash.write_text(build_dashboard(findings, plan, results))
    shutil.copy(dash, REPORTS / "demo1-burndown.html")
    (REPORTS / "demo1-remediation-report.md").write_text(build_report(findings, plan, results))
    log(f"dashboard -> {dash}")
    log(f"report    -> {REPORTS / 'demo1-remediation-report.md'}")


if __name__ == "__main__":
    main()
