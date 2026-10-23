#!/usr/bin/env bash
# Arden live demos (fleetwide, incident, audit, t1): (re)create the BEFORE branch a presenter hands to Devin.
# BEFORE = the estate branch with every pre-computed answer removed (reports, deterministic stand-ins, docs),
# plus the audit control list. The estate repos in STPS and Springboot-BankApp are already at the BEFORE state.
# Safe to rerun: force-resets ONLY the seed branch and closes PRs that targeted it.
set -eEuo pipefail

REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
SEED_BRANCH="${DEMO_ARDEN_SEED_BRANCH:-demo/arden-live-before}"
ESTATE_BRANCH=devin/1790864747-arden-finserv-estate
AUDIT_BRANCH=devin/fs-audit-1791848400-ccc-traceability
LIVE=$(cd "$(dirname "$0")" && pwd)
GIT_ROOT=$(git -C "$LIVE" rev-parse --show-toplevel)
CURRENT_STEP="initializing"
WORKTREE=""

trap 'status=$?; printf "SEED FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
trap '[[ -n "$WORKTREE" ]] && git -C "$GIT_ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1; true' EXIT
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

case "$SEED_BRANCH" in main|master|devin/*) fail "refusing to reset branch $SEED_BRANCH";; esac

step "Tools"
for t in git gh python3; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

step "Close PRs left on $SEED_BRANCH by earlier runs"
gh pr list -R "$REPO" --base "$SEED_BRANCH" --state open --json number,headRefName --jq '.[] | "\(.number) \(.headRefName)"' |
while read -r num head; do
  gh pr close -R "$REPO" "$num" --comment "Closed by demo/live/seed-situation.sh (reseeding $SEED_BRANCH)." >/dev/null
  [[ "$head" == devin/* ]] && git -C "$GIT_ROOT" push -q origin --delete "$head" 2>/dev/null || true
  echo "    closed #$num ($head)"
done

step "Fetch $ESTATE_BRANCH and $AUDIT_BRANCH"
git -C "$GIT_ROOT" fetch -q origin "$ESTATE_BRANCH" "$AUDIT_BRANCH"
ESTATE_SHA=$(git -C "$GIT_ROOT" rev-parse "origin/$ESTATE_BRANCH")
AUDIT_SHA=$(git -C "$GIT_ROOT" rev-parse "origin/$AUDIT_BRANCH")

step "Build BEFORE tree from $ESTATE_SHA"
WORKTREE=$(mktemp -d)
rmdir "$WORKTREE"
git -C "$GIT_ROOT" worktree add -q --detach "$WORKTREE" "$ESTATE_SHA"
K="$WORKTREE/arden/finserv-estate"
git -C "$WORKTREE" rm -rq --ignore-unmatch \
  arden/finserv-estate/demo/reports \
  arden/finserv-estate/docs \
  arden/finserv-estate/.agents \
  arden/finserv-estate/demo/scanner/remediate.py \
  arden/finserv-estate/demo/incident/investigate.py \
  arden/finserv-estate/demo/incident/report.py \
  arden/finserv-estate/demo/run-demo1.sh \
  arden/finserv-estate/demo/run-demo2.sh \
  arden/finserv-estate/demo/run-demo3.sh \
  arden/finserv-estate/demo/t1/inventory.py \
  arden/finserv-estate/demo/t1/migrate.py
mkdir -p "$K/demo/audit/fixtures"
for f in ccc-core-v2025.10.yaml audit-calendar.yaml; do
  git -C "$GIT_ROOT" show "$AUDIT_SHA:arden/finserv-estate/demo/audit/fixtures/$f" > "$K/demo/audit/fixtures/$f"
done
cp "$LIVE/AGENTS.md" "$WORKTREE/AGENTS.md"

step "Verify no answers are on the BEFORE tree"
for p in demo/reports docs demo/scanner/remediate.py demo/incident/investigate.py demo/t1/migrate.py demo/live; do
  [[ ! -e "$K/$p" ]] || fail "answer path still present: $p"
done
for p in demo/scanner/fixtures/snyk-webhook.json demo/incident/fixtures/datadog/monitor-webhook.json \
         demo/t1/golden/trades-2027Q4.csv demo/audit/fixtures/ccc-core-v2025.10.yaml demo/playbooks/fs-cve-remediation-child.md; do
  [[ -s "$K/$p" ]] || fail "fixture missing: $p"
done
grep -q "px: q.px" "$WORKTREE/arden/client-portal/src/pricing/normalize.js" || fail "client-portal is not at the v2.2-only parser"
git -C "$WORKTREE" add -A
git -C "$WORKTREE" -c user.name="Devin AI" -c user.email="158243242+devin-ai-integration[bot]@users.noreply.github.com" \
  commit -q -m "chore: seed Arden live BEFORE state (answers removed, audit control list added)"

step "Push $SEED_BRANCH"
git -C "$WORKTREE" push -q --force origin "HEAD:refs/heads/$SEED_BRANCH"
LOCAL=$(git -C "$WORKTREE" rev-parse HEAD)
REMOTE=$(git -C "$GIT_ROOT" ls-remote origin "refs/heads/$SEED_BRANCH" | cut -f1)
[[ "$LOCAL" == "$REMOTE" ]] || fail "remote $SEED_BRANCH is $REMOTE, expected $LOCAL"

cat <<BANNER

READY: Arden live situation seeded
  Seed branch: https://github.com/$REPO/tree/$SEED_BRANCH  ($LOCAL)
  Estate repos (unchanged, BEFORE state): STPS, traderX, Springboot-BankApp @ $ESTATE_BRANCH
  Prompts:     arden/finserv-estate/demo/live/PROMPTS.md (on the kit branch)
  Fallback:    git checkout $ESTATE_BRANCH && arden/finserv-estate/demo/run-demo<N>.sh
BANNER
