#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ESTATE="$(cd "$HERE/.." && pwd)"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
PORTAL_MAIN="$WS/client-portal"
FIX_BRANCH="incident/inc-2026-0925-01-ardenfeed-v23"
FIX_WORKTREE="${FS_PORTAL_WORKTREE:-$WS/../client-portal-incident-fix}"
URL="http://localhost:3210/"
LOG_DIR="$HOME/.cache"

die() {
  printf 'PORTAL FAILED: %s\n' "$*" >&2
  exit 1
}

stop_server() {
  fuser -k 3210/tcp >/dev/null 2>&1 || true
  for _ in $(seq 1 20); do
    fuser -n tcp 3210 >/dev/null 2>&1 || return 0
    sleep 0.1
  done
  die "port 3210 remained occupied after stop"
}

remove_fix_worktree() {
  if [[ -e "$FIX_WORKTREE" ]]; then
    [[ -d "$PORTAL_MAIN" ]] || die "cannot remove fix worktree without $PORTAL_MAIN"
    git -C "$PORTAL_MAIN" worktree remove "$FIX_WORKTREE" || die "could not remove fix worktree at $FIX_WORKTREE"
  fi
}

start_server() {
  local mode="$1"
  local directory="$2"
  local page_file="$LOG_DIR/arden-portal-${mode}.html"
  local api_file="$LOG_DIR/arden-portal-${mode}-api.json"
  local log_file="$LOG_DIR/arden-portal-${mode}.log"
  local page_status=""
  local api_status=""
  local ready=0

  mkdir -p "$LOG_DIR"
  (cd "$directory" && nohup env PORT=3210 ARDENFEED_BATCH=../market-data-feed-contracts/samples/latest-batch.json \
    node src/server.js >"$log_file" 2>&1 </dev/null &)

  for _ in $(seq 1 40); do
    page_status="$(curl -sS --max-time 1 -o "$page_file" -w '%{http_code}' "$URL" 2>/dev/null || true)"
    api_status="$(curl -sS --max-time 1 -o "$api_file" -w '%{http_code}' "${URL}api/portfolio/valuation" 2>/dev/null || true)"
    if [[ "$page_status" == "200" ]] && grep -q "Arden Capital Markets" "$page_file"; then
      if [[ "$mode" == "before" ]] && [[ "$api_status" == "500" ]] && grep -q "valuation_failed" "$api_file"; then
        ready=1
        break
      elif [[ "$mode" == "after" ]] && [[ "$api_status" == "200" ]] && \
        python3 - "$api_file" <<'PY'
import json
import pathlib
import sys
body = json.loads(pathlib.Path(sys.argv[1]).read_text())
raise SystemExit(0 if len(body.get("rows", [])) == 3 else 1)
PY
      then
        ready=1
        break
      fi
    fi
    sleep 0.25
  done

  if (( ! ready )); then
    stop_server
    printf 'Portal did not become ready. Log: %s\n' "$log_file" >&2
    [[ ! -f "$log_file" ]] || cat "$log_file" >&2
    die "expected GET / 200 and the ${mode} valuation API response"
  fi

  if [[ "$mode" == "before" ]]; then
    printf 'Portal ready: %s\nPresenter should see: Valuation unavailable (HTTP 500: valuation_failed).\n' "$URL"
  else
    printf 'Portal ready: %s\nPresenter should see: the incident fix branch with three valued rows and an as-of date.\n' "$URL"
  fi
}

case "${1:-}" in
  before)
    [[ -d "$PORTAL_MAIN" ]] || die "workspace client-portal is missing; run scripts/reset.sh first"
    branch="$(git -C "$PORTAL_MAIN" branch --show-current)"
    if [[ "$branch" != "main" ]]; then
      [[ -z "$(git -C "$PORTAL_MAIN" status --porcelain)" ]] || die "client-portal is not clean; refusing to switch to main"
      git -C "$PORTAL_MAIN" checkout -q main
    fi
    stop_server
    remove_fix_worktree
    start_server before "$PORTAL_MAIN"
    ;;
  after)
    [[ -d "$PORTAL_MAIN" ]] || die "workspace client-portal is missing; run scripts/reset.sh first"
    git -C "$PORTAL_MAIN" show-ref --verify --quiet "refs/heads/$FIX_BRANCH" || die "incident fix branch is missing: $FIX_BRANCH"
    stop_server
    remove_fix_worktree
    if [[ "$(git -C "$PORTAL_MAIN" worktree list --porcelain)" == *"branch refs/heads/$FIX_BRANCH"* ]]; then
      git -C "$PORTAL_MAIN" worktree add --detach -q "$FIX_WORKTREE" "$FIX_BRANCH"
    else
      git -C "$PORTAL_MAIN" worktree add -q "$FIX_WORKTREE" "$FIX_BRANCH"
    fi
    start_server after "$FIX_WORKTREE"
    ;;
  stop)
    stop_server
    remove_fix_worktree
    printf 'Portal stopped: %s (no page is being served).\n' "$URL"
    ;;
  *)
    printf 'Usage: %s before|after|stop\n' "$0" >&2
    exit 2
    ;;
esac
