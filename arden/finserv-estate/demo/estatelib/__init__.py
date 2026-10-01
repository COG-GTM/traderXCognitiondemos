"""Shared helpers for the three demo drivers (stdlib only)."""
from __future__ import annotations

import fnmatch
import json
import os
import pathlib
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone

os.environ.setdefault("PYTHONDONTWRITEBYTECODE", "1")

ESTATE = pathlib.Path(__file__).resolve().parents[2]
REPOS = ESTATE / "repos"  # local (tarball) layout; absent when the kit runs from COG-GTM/traderXCognitiondemos
SOURCES = pathlib.Path(os.environ.get("FS_SOURCES", pathlib.Path.home() / "finserv-sources"))
REPO_MAP = ESTATE / "scripts" / "repo-map.tsv"
WORKSPACE = pathlib.Path(os.environ.get("FS_WORKSPACE", pathlib.Path.home() / "finserv-workspace"))
OUT = ESTATE / "demo" / "out"
REPORTS = ESTATE / "demo" / "reports"
JAVA17 = os.environ.get("FS_JAVA_HOME", "/usr/lib/jvm/java-17-openjdk-amd64")


@dataclass(frozen=True)
class RepoHome:
    """Where an estate repo lives in the COG-GTM org (scripts/repo-map.tsv)."""
    repo: str
    github: str
    ref: str
    subdir: str

    @property
    def url(self) -> str:
        return f"https://github.com/{self.github}"

    @property
    def tree_url(self) -> str:
        return f"{self.url}/tree/{self.ref}/{self.subdir}"

    @property
    def clone_dir(self) -> pathlib.Path:
        return SOURCES / self.github.split("/")[-1]


def repo_map() -> dict[str, RepoHome]:
    homes: dict[str, RepoHome] = {}
    override = os.environ.get("FS_SOURCE_REF")
    for line in REPO_MAP.read_text().splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        repo, github, ref, subdir = line.split("\t")[:4]
        homes[repo] = RepoHome(repo, github, override or ref, subdir)
    return homes


def repo_home(repo: str) -> RepoHome:
    return repo_map()[repo]


def source_dir(repo: str) -> pathlib.Path:
    """Pristine source tree for an estate repo: local repos/ when present, else the mapped COG-GTM clone."""
    local = REPOS / repo
    if local.is_dir():
        return local
    home = repo_home(repo)
    return home.clone_dir / home.subdir


def target_line(repo: str) -> str:
    """One PR-body line saying where the real PR lands (scripts/publish-branch.sh pushes there)."""
    h = repo_home(repo)
    return f"**Lands in:** [{h.github}]({h.url}) under `{h.subdir}/` (base `{h.ref}`) — publish with `scripts/publish-branch.sh {repo} <branch> <patch>`"


def log(msg: str) -> None:
    print(f"\033[1;36m[demo]\033[0m {msg}", flush=True)


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def java_env() -> dict[str, str]:
    env = dict(os.environ)
    env["JAVA_HOME"] = JAVA17
    env["PATH"] = f"{JAVA17}/bin:" + env["PATH"]
    return env


@dataclass
class CmdResult:
    cmd: str
    cwd: str
    returncode: int
    stdout: str

    @property
    def ok(self) -> bool:
        return self.returncode == 0


def run(cmd: list[str] | str, cwd: pathlib.Path, env: dict[str, str] | None = None, check: bool = False,
        stdin: str | None = None) -> CmdResult:
    shell = isinstance(cmd, str)
    p = subprocess.run(cmd, cwd=cwd, env=env or java_env(), shell=shell, text=True, input=stdin,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    res = CmdResult(cmd if shell else " ".join(cmd), str(cwd), p.returncode, p.stdout)
    if check and not res.ok:
        sys.stderr.write(res.stdout[-4000:])
        raise SystemExit(f"command failed in {cwd}: {res.cmd}")
    return res


def git(repo: pathlib.Path, *args: str) -> CmdResult:
    return run(["git", "-c", "user.name=Devin", "-c", "user.email=devin@ardencm.example", *args], repo)


def write_json(path: pathlib.Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, default=str) + "\n")


def read_json(path: pathlib.Path):
    return json.loads(path.read_text())


# ---------------------------------------------------------------------------------------------------
# CODEOWNERS
# ---------------------------------------------------------------------------------------------------
def load_codeowners(repo_dir: pathlib.Path) -> list[tuple[str, list[str]]]:
    rules: list[tuple[str, list[str]]] = []
    for cand in ("CODEOWNERS", ".github/CODEOWNERS"):
        p = repo_dir / cand
        if p.exists():
            for line in p.read_text().splitlines():
                line = line.split("#", 1)[0].strip()
                if not line:
                    continue
                pattern, *owners = line.split()
                rules.append((pattern, owners))
            break
    return rules


def owners_for(repo_dir: pathlib.Path, rel_path: str) -> list[str]:
    """Last matching rule wins, as in GitHub."""
    match: list[str] = []
    for pattern, owners in load_codeowners(repo_dir):
        pat = pattern
        if pat == "*":
            match = owners
            continue
        anchored = pat.startswith("/")
        pat = pat.lstrip("/")
        if pat.endswith("/"):
            if rel_path.startswith(pat) or (not anchored and f"/{pat}" in f"/{rel_path}"):
                match = owners
        elif fnmatch.fnmatch(rel_path, pat) or fnmatch.fnmatch(rel_path, f"*/{pat}") or rel_path == pat:
            match = owners
    return match


# ---------------------------------------------------------------------------------------------------
# HTML theme for generated dashboards / reports (one-off pages)
# ---------------------------------------------------------------------------------------------------
HTML_HEAD = """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
:root{{--bg:#0b0f17;--panel:rgba(255,255,255,.04);--line:rgba(255,255,255,.08);--fg:#e6e9ef;--muted:#8b93a7;--accent:#5eead4;--accent2:#818cf8;--warn:#fbbf24;--bad:#f87171;--good:#34d399}}
*{{box-sizing:border-box}}body{{margin:0;background:radial-gradient(1200px 600px at 10% -10%,rgba(129,140,248,.18),transparent),radial-gradient(900px 500px at 100% 0,rgba(94,234,212,.12),transparent),var(--bg);color:var(--fg);font:15px/1.55 Inter,system-ui,sans-serif}}
main{{max-width:1180px;margin:0 auto;padding:48px 28px 80px}}h1{{font-size:30px;font-weight:700;letter-spacing:-.02em;margin:0 0 6px}}h2{{font-size:18px;font-weight:600;margin:40px 0 14px;letter-spacing:-.01em}}
.sub{{color:var(--muted);margin:0 0 28px}}.grid{{display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(200px,1fr))}}
.card{{background:var(--panel);border:1px solid var(--line);border-radius:16px;padding:18px 20px;backdrop-filter:blur(8px)}}.card .k{{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.08em}}.card .v{{font-size:28px;font-weight:700;margin-top:6px}}
table{{width:100%;border-collapse:collapse;font-size:13.5px}}th,td{{text-align:left;padding:10px 12px;border-bottom:1px solid var(--line);vertical-align:top}}th{{color:var(--muted);font-weight:500;font-size:12px;text-transform:uppercase;letter-spacing:.06em}}
code,.mono{{font-family:"JetBrains Mono",ui-monospace,monospace;font-size:12.5px}}pre{{background:rgba(0,0,0,.35);border:1px solid var(--line);border-radius:12px;padding:14px 16px;overflow:auto;font-size:12.5px}}
.pill{{display:inline-block;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:600;border:1px solid var(--line)}}.pill.bad{{color:var(--bad);border-color:rgba(248,113,113,.4)}}.pill.warn{{color:var(--warn);border-color:rgba(251,191,36,.4)}}.pill.good{{color:var(--good);border-color:rgba(52,211,153,.4)}}.pill.info{{color:var(--accent2);border-color:rgba(129,140,248,.4)}}
a{{color:var(--accent)}}.foot{{color:var(--muted);font-size:12px;margin-top:48px;border-top:1px solid var(--line);padding-top:16px}}
svg text{{fill:var(--muted);font-size:11px;font-family:Inter}}
</style></head><body><main>
"""
HTML_FOOT = """<p class="foot">{foot}</p></main></body></html>"""


def html_page(title: str, body: str, foot: str) -> str:
    return HTML_HEAD.format(title=title) + body + HTML_FOOT.format(foot=foot)


def esc(s) -> str:
    return str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
