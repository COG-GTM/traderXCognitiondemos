#!/usr/bin/env bash
# demo_incident: seed INC-2026-0925-01 (client-portal valuation 5xx), prepare this machine, verify the BEFORE state,
# serve the broken portal and print READY with the seed branch URL, prompt 1 and the fallback.
#   bash setup.sh               reseed + verify + serve BEFORE portal on :3200
#   DEMO_INCIDENT_SKIP_SEED=1   verify + serve only (do not force-reset the shared seed branch)
#   bash setup.sh --fallback    serve the pre-built fix (PR #122) on :3201 instead
set -eEuo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
KIT_BRANCH="${DEMO_ARDEN_KIT_BRANCH:-devin/1792724600-demo-arden-live}"
SEED_BRANCH="${DEMO_ARDEN_SEED_BRANCH:-demo/arden-live-before}"
FALLBACK_PR=122
FALLBACK_PR_ALT=118
PLAYBOOK_ID="playbook-abc2582e18fd45558d56838a14101336"
CLONE="${DEMO_INCIDENT_CLONE:-$HOME/repos/traderXCognitiondemos}"
WORK="${DEMO_INCIDENT_WORK:-$HOME/.demo_incident}"
PORT="${DEMO_INCIDENT_PORT:-3200}"
FALLBACK_PORT="${DEMO_INCIDENT_FALLBACK_PORT:-3201}"
TEST_PORT="${DEMO_INCIDENT_TEST_PORT:-3290}"
MODE=live
[[ "${1:-}" == "--fallback" ]] && MODE=fallback
CURRENT_STEP="initializing"
TEST_PID=""

trap 'status=$?; printf "SETUP FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
trap '[[ -n "$TEST_PID" ]] && kill "$TEST_PID" 2>/dev/null; true' EXIT
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; printf 'ERROR: %s\n' "$1" >&2; false; }

is_portal() {  # pid is a client-portal server: cmdline runs src/server.js and cwd is arden/client-portal
  [[ "$1" =~ ^[0-9]+$ && -r "/proc/$1/cmdline" ]] || return 1
  tr '\0' ' ' <"/proc/$1/cmdline" 2>/dev/null | grep -q 'src/server.js' || return 1
  local cwd; cwd=$(readlink "/proc/$1/cwd" 2>/dev/null) || return 1
  [[ "${cwd% (deleted)}" == */arden/client-portal ]]  # "(deleted)": its worktree was recreated by a rerun
}
port_free() { ! (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }
stop_port() {  # stop a client-portal we started earlier on this port
  local pidfile="$WORK/portal-$1.pid"
  if [[ -f "$pidfile" ]]; then
    local pid; pid=$(cat "$pidfile")
    if is_portal "$pid"; then kill "$pid" 2>/dev/null || true; sleep 1; else echo "    stale pid file for :$1 (pid $pid is not the portal), not killing"; fi
    rm -f "$pidfile"
  fi
  port_free "$1" || fail "port $1 is in use by another process"
}
wait_up() { for _ in $(seq 1 50); do curl -fs "http://127.0.0.1:$1/health" >/dev/null 2>&1 && return 0; sleep 0.2; done; return 1; }
worktree_at() {  # worktree_at <dir> <ref>
  [[ -e "$1" ]] && git -C "$CLONE" worktree remove --force "$1" >/dev/null 2>&1 || true
  rm -rf "$1"; git -C "$CLONE" worktree prune
  git -C "$CLONE" worktree add -q --detach "$1" "$2"
}
serve() {  # serve <portal dir> <port> <batch>: detached server that survives this shell
  (cd "$1" && ARDENFEED_BATCH="$3" PORT="$2" exec setsid node src/server.js) >"$WORK/portal-$2.log" 2>&1 </dev/null &
  echo $! >"$WORK/portal-$2.pid"
  wait_up "$2" || fail "client-portal did not start on port $2 (see $WORK/portal-$2.log)"
}
retry() { local n; for n in 1 2 3 4; do "$@" && return 0; echo "    retry $n: $*" >&2; sleep $((n * 3)); done; "$@"; }
prompt1() { awk '/<!-- prompt-1:start -->/{f=1;next} /<!-- prompt-1:end -->/{f=0} f' "$HERE/PROMPTS.md" | sed '/^```/d'; }

step "Tools"
for t in git gh curl awk; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated (gh auth login, or set GH_TOKEN)"
[[ -s "$HERE/PROMPTS.md" ]] || fail "PROMPTS.md missing next to setup.sh"
[[ -n "$(prompt1)" ]] || fail "prompt 1 not found in PROMPTS.md"
mkdir -p "$WORK"

step "Node 20+"
node_major() { node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0; }
if (( $(node_major) < 20 )); then
  sudo -n true 2>/dev/null || fail "Node 20+ missing and sudo needs a password; install Node 20 and rerun"
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - >"$WORK/node-install.log" 2>&1
  sudo apt-get install -y nodejs >>"$WORK/node-install.log" 2>&1
fi
(( $(node_major) >= 20 )) || fail "Node $(node -v 2>/dev/null || echo none) is below 20"

step "Checkout $REPO at $CLONE"
if [[ ! -d "$CLONE/.git" ]]; then
  mkdir -p "$(dirname "$CLONE")"
  retry gh repo clone "$REPO" "$CLONE" -- -q
fi
retry git -C "$CLONE" fetch -q origin \
  "+refs/heads/$KIT_BRANCH:refs/remotes/origin/$KIT_BRANCH" \
  "+refs/pull/$FALLBACK_PR/head:refs/remotes/origin/pr-$FALLBACK_PR" \
  "+refs/pull/$FALLBACK_PR_ALT/head:refs/remotes/origin/pr-$FALLBACK_PR_ALT"

if [[ "$MODE" == "live" && "${DEMO_INCIDENT_SKIP_SEED:-0}" != "1" ]]; then
  step "Seed $SEED_BRANCH (shared Arden live seed)"
  worktree_at "$WORK/kit" "origin/$KIT_BRANCH"
  DEMO_ARDEN_REPO="$REPO" DEMO_ARDEN_SEED_BRANCH="$SEED_BRANCH" \
    bash "$WORK/kit/arden/finserv-estate/demo/live/seed-situation.sh" | sed 's/^READY:/    seed done:/'
fi

step "Fetch $SEED_BRANCH"
retry git -C "$CLONE" fetch -q origin "+refs/heads/$SEED_BRANCH:refs/remotes/origin/$SEED_BRANCH"
SEED_SHA=$(git -C "$CLONE" rev-parse "origin/$SEED_BRANCH")
worktree_at "$WORK/before" "origin/$SEED_BRANCH"
P="$WORK/before/arden/client-portal"
K="$WORK/before/arden/finserv-estate"

step "Verify BEFORE: the fix and the answers are not on $SEED_BRANCH"
grep -q "px: q.px" "$P/src/pricing/normalize.js" || fail "normalizeQuote is not the v2.2-only parser"
! grep -q "px.v" "$P/src/pricing/normalize.js" || fail "normalizeQuote already handles v2.3 (fix is on the seed)"
for p in demo/incident/investigate.py demo/incident/report.py demo/run-demo3.sh demo/reports docs; do
  [[ ! -e "$K/$p" ]] || fail "answer path still on the seed: $p"
done
for f in monitor-webhook.json logs-search.json apm-error-trace.json deploy-events.json; do
  [[ -s "$K/demo/incident/fixtures/datadog/$f" ]] || fail "fixture missing: demo/incident/fixtures/datadog/$f"
done
for f in fixtures/ardenfeed-v2.2.json fixtures/ardenfeed-v2.3.json deploys/history.json; do
  [[ -s "$P/$f" ]] || fail "client-portal fixture missing: $f"
done
[[ -s "$WORK/before/arden/market-data-feed-contracts/ardenfeed/VENDOR-NOTICE-2026-09-11.md" ]] || fail "vendor notice missing"
OPEN_PRS=$(gh pr list -R "$REPO" --base "$SEED_BRANCH" --state open --json number --jq 'length')
if [[ "$OPEN_PRS" != "0" ]]; then
  echo "    note: $OPEN_PRS open PR(s) already target $SEED_BRANCH (from an earlier run); rerun without DEMO_INCIDENT_SKIP_SEED to clear them"
fi

step "Verify BEFORE: tests pass on v2.2 and the v2.3 batch fails like production"
(cd "$P" && npm test >"$WORK/npm-test.log" 2>&1) || fail "npm test fails on the seed (see $WORK/npm-test.log)"
port_free "$TEST_PORT" || fail "test port $TEST_PORT in use"
(cd "$P" && ARDENFEED_BATCH=fixtures/ardenfeed-v2.3.json PORT="$TEST_PORT" exec node src/server.js) >"$WORK/repro.log" 2>&1 </dev/null &
TEST_PID=$!
wait_up "$TEST_PORT" || fail "client-portal did not start for the repro"
CODE=$(curl -s -o "$WORK/repro.json" -w '%{http_code}' "http://127.0.0.1:$TEST_PORT/api/portfolio/valuation")
kill "$TEST_PID"; TEST_PID=""
[[ "$CODE" == "500" ]] && grep -q valuation_failed "$WORK/repro.json" || fail "v2.3 batch returned $CODE, expected 500 valuation_failed"
grep -q "n.px.toFixed is not a function" "$WORK/repro.log" || fail "server log lacks the production TypeError"

if [[ "$MODE" == "fallback" ]]; then
  step "Serve the pre-built fix (PR #$FALLBACK_PR) on :$FALLBACK_PORT"
  worktree_at "$WORK/fallback" "origin/pr-$FALLBACK_PR"
  stop_port "$FALLBACK_PORT"
  serve "$WORK/fallback/arden/client-portal" "$FALLBACK_PORT" fixtures/ardenfeed-v2.3.json
  curl -fs "http://127.0.0.1:$FALLBACK_PORT/api/portfolio/valuation" | grep -q '"marketValue"' || fail "fallback portal did not value the v2.3 batch"
  cat <<BANNER

READY: demo_incident fallback (pre-built fix, not live)
  After portal:  http://localhost:$FALLBACK_PORT/api/portfolio/valuation  (PR #$FALLBACK_PR head, ArdenFeed v2.3 batch, valued rows)
  Fix PR:        https://github.com/$REPO/pull/$FALLBACK_PR   (alternate: https://github.com/$REPO/pull/$FALLBACK_PR_ALT)
  Say:           "This is the fix Devin produced on an earlier run of the same incident."
BANNER
  exit 0
fi

step "Serve the BEFORE portal on :$PORT (ArdenFeed v2.3 batch, as in production)"
stop_port "$PORT"
serve "$P" "$PORT" fixtures/ardenfeed-v2.3.json

cat <<BANNER

READY: demo_incident (INC-2026-0925-01, client-portal valuation 5xx before the London open)
  Seed branch:   https://github.com/$REPO/tree/$SEED_BRANCH  ($SEED_SHA)
  BEFORE portal: http://localhost:$PORT/api/portfolio/valuation  -> 500 {"error":"valuation_failed"}
                 http://localhost:$PORT/api/settlement/summary   -> 200 (unaffected page)
  Devin:         new session on $REPO, playbook $PLAYBOOK_ID (FS incident response)
  Prompt 1 (paste as is; also in $HERE/PROMPTS.md):
$(prompt1 | sed 's/^/    /')
  Fallback:      bash $HERE/setup.sh --fallback   (serves PR #$FALLBACK_PR on :$FALLBACK_PORT)
                 https://github.com/$REPO/pull/$FALLBACK_PR  and  https://github.com/$REPO/pull/$FALLBACK_PR_ALT
BANNER
