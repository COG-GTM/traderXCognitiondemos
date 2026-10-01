#!/usr/bin/env python3
"""Scanner webhook receiver — the trigger for Demo 1.

    python3 demo/scanner/webhook.py --port 8787            # listen
    curl -XPOST localhost:8787/hooks/snyk \\
         -H 'X-Hub-Signature: sha256=<hmac>' --data @demo/scanner/fixtures/snyk-webhook.json

On a valid POST it writes the payload to demo/out/scanner/inbox/<ts>.json and runs the parent pipeline
(normalize → fanout → remediate → report) in the mode given by FS_FANOUT_MODE (dry-run | live).
The signature check mirrors Snyk's `X-Hub-Signature` (HMAC-SHA256 of the raw body with the webhook secret,
env FS_WEBHOOK_SECRET; default fixture secret "demo").  With a real Snyk org, point the webhook here and set
FS_SCANNER_SOURCE=snyk-api so the parent re-reads findings from the API instead of trusting the payload.
"""
from __future__ import annotations

import argparse
import hashlib
import hmac
import http.server
import json
import os
import pathlib
import subprocess
import sys
import threading

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import ESTATE, OUT, log, now_iso  # noqa: E402

SECRET = os.environ.get("FS_WEBHOOK_SECRET", "demo").encode()
MODE = os.environ.get("FS_FANOUT_MODE", "dry-run")
SOURCE = os.environ.get("FS_SCANNER_SOURCE", "snyk-webhook")
PY = sys.executable


def run_parent(payload_path: pathlib.Path) -> None:
    steps = [
        [PY, "demo/scanner/normalize.py", "--source", SOURCE, "--input", str(payload_path)],
        [PY, "demo/scanner/fanout.py", "--mode", MODE],
        [PY, "demo/scanner/remediate.py"],
        [PY, "demo/scanner/report.py"],
    ]
    for s in steps:
        log("parent: " + " ".join(s[1:]))
        if subprocess.run(s, cwd=ESTATE).returncode != 0:
            log("parent: step failed, stopping")
            return
    log("parent: done — demo/out/scanner/burndown.html")


class Handler(http.server.BaseHTTPRequestHandler):
    def do_POST(self) -> None:  # noqa: N802
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        sig = self.headers.get("X-Hub-Signature", "")
        expected = "sha256=" + hmac.new(SECRET, body, hashlib.sha256).hexdigest()
        if not hmac.compare_digest(sig, expected):
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b"bad signature\n")
            return
        try:
            payload = json.loads(body)
        except json.JSONDecodeError:
            self.send_response(400)
            self.end_headers()
            return
        inbox = OUT / "scanner/inbox"
        inbox.mkdir(parents=True, exist_ok=True)
        p = inbox / (now_iso().replace(":", "") + ".json")
        p.write_text(json.dumps(payload, indent=2))
        log(f"webhook accepted {self.path} ({len(body)} bytes) -> {p}")
        threading.Thread(target=run_parent, args=(p,), daemon=True).start()
        self.send_response(202)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"accepted": True, "parent_mode": MODE, "payload": str(p)}).encode())

    def log_message(self, *_args) -> None:
        pass


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8787)
    ap.add_argument("--sign", metavar="FILE", help="print the X-Hub-Signature for FILE and exit")
    a = ap.parse_args()
    if a.sign:
        print("sha256=" + hmac.new(SECRET, pathlib.Path(a.sign).read_bytes(), hashlib.sha256).hexdigest())
        return
    log(f"listening on :{a.port}  mode={MODE} source={SOURCE}")
    http.server.ThreadingHTTPServer(("127.0.0.1", a.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
