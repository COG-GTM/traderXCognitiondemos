#!/usr/bin/env bash
# demo_t1: (re)create the T+1 BEFORE branch a presenter hands to Devin, and clean up an earlier live run.
#   BEFORE = the shared Arden seed (demo/arden-live-before: no inventory, no migration, no reports)
#            + the kit's demo/t1/parity.py (standalone: --baseline/--migrated), so the presenter can check Devin's jar.
# Never touches the shared seed branch, the estate branch, or main. Safe to rerun.
set -eEuo pipefail

REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
SHARED_SEED="${DEMO_ARDEN_SEED_BRANCH:-demo/arden-live-before}"
T1_SEED="${DEMO_T1_SEED_BRANCH:-demo/t1-live-before}"
TICKET="${DEMO_T1_TICKET:-T1-2027}"
ESTATE_REPOS=(COG-GTM/Securities-Trade-Processing-System COG-GTM/traderXCognitiondemos COG-GTM/Springboot-BankApp)
# Standing reference PRs the presenter shows (PRESENTER.md); never closed by a reseed.
STPS=COG-GTM/Securities-Trade-Processing-System
KEEP_PRS=" $STPS#2 $STPS#3 $STPS#4 COG-GTM/traderXCognitiondemos#134 ${DEMO_T1_KEEP_PRS:-} "
HERE=$(cd "$(dirname "$0")" && pwd)
KIT=$(cd "$HERE/../../.." && pwd)              # arden/finserv-estate on the kit branch
GIT_ROOT=$(git -C "$HERE" rev-parse --show-toplevel)
CURRENT_STEP="initializing"
WORKTREE=""

trap 'status=$?; printf "SEED FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
trap '[[ -n "$WORKTREE" ]] && git -C "$GIT_ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1; true' EXIT
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

case "$T1_SEED" in main|master|devin/*|"$SHARED_SEED") fail "refusing to reset branch $T1_SEED";; esac

step "Tools"
for t in git gh python3; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

step "Close open PRs from earlier $TICKET runs (opened after the previous seed)"
PREV_SEED=""
if git -C "$GIT_ROOT" fetch -q origin "$T1_SEED" 2>/dev/null; then
  PREV_SEED=$(git -C "$GIT_ROOT" log -1 --format=%cI FETCH_HEAD)
fi
if [[ -z "$PREV_SEED" ]]; then
  echo "    no previous $T1_SEED: nothing to clean up"
else
  for r in "${ESTATE_REPOS[@]}"; do
    gh pr list -R "$r" --state open --search "$TICKET in:title,body created:>=$PREV_SEED" --json number,headRefName \
       --jq '.[] | select(.headRefName | startswith("devin/")) | "\(.number) \(.headRefName)"' |
    while read -r num head; do
      [[ "$KEEP_PRS" == *" $r#$num "* ]] && { echo "    kept $r#$num (reference PR)"; continue; }
      gh pr close -R "$r" "$num" --delete-branch --comment "Closed by demo/live/t1/seed-t1.sh (reseeding the $TICKET demo)." >/dev/null
      echo "    closed $r#$num ($head)"
    done
  done
fi
gh pr list -R "$REPO" --base "$T1_SEED" --state open --json number --jq '.[].number' |
while read -r num; do [[ "$KEEP_PRS" == *" $REPO#$num "* ]] && continue; gh pr close -R "$REPO" "$num" --comment "Closed by seed-t1.sh (reseeding $T1_SEED)." >/dev/null; echo "    closed #$num"; done

step "Fetch $SHARED_SEED"
if ! git -C "$GIT_ROOT" fetch -q origin "$SHARED_SEED" 2>/dev/null; then
  step "Shared seed missing: run demo/live/seed-situation.sh"
  "$HERE/../seed-situation.sh"
  git -C "$GIT_ROOT" fetch -q origin "$SHARED_SEED"
fi
BASE_SHA=$(git -C "$GIT_ROOT" rev-parse "origin/$SHARED_SEED")

step "Build $T1_SEED from $SHARED_SEED@${BASE_SHA:0:7}"
WORKTREE=$(mktemp -d); rmdir "$WORKTREE"
git -C "$GIT_ROOT" worktree add -q --detach "$WORKTREE" "$BASE_SHA"
K="$WORKTREE/arden/finserv-estate"
cp "$KIT/demo/t1/parity.py" "$K/demo/t1/parity.py"

step "Verify the BEFORE tree carries no T+1 answers"
for p in demo/t1/inventory.py demo/t1/migrate.py demo/reports demo/out demo/live; do
  [[ ! -e "$K/$p" ]] || fail "answer path present on seed: $p"
done
for p in demo/t1/golden/trades-2027Q4.csv demo/t1/golden/dividend-events-2027Q4.csv demo/t1/parity.py demo/playbooks/fs-t1-migration-tranche.md; do
  [[ -s "$K/$p" ]] || fail "fixture missing: $p"
done
grep -q -- "--migrated" "$K/demo/t1/parity.py" || fail "parity.py is not the standalone version"
[[ -s "$WORKTREE/AGENTS.md" ]] || fail "AGENTS.md missing on seed"
if ! git -C "$WORKTREE" diff --quiet; then
  git -C "$WORKTREE" add -A
  git -C "$WORKTREE" -c user.name="Devin AI" -c user.email="158243242+devin-ai-integration[bot]@users.noreply.github.com" \
    commit -q -m "chore: seed T+1 live BEFORE state (standalone parity check)"
fi

step "Push $T1_SEED"
git -C "$WORKTREE" push -q --force origin "HEAD:refs/heads/$T1_SEED"
LOCAL=$(git -C "$WORKTREE" rev-parse HEAD)
REMOTE=$(git -C "$GIT_ROOT" ls-remote origin "refs/heads/$T1_SEED" | cut -f1)
[[ "$LOCAL" == "$REMOTE" ]] || fail "remote $T1_SEED is $REMOTE, expected $LOCAL"
echo "SEEDED: https://github.com/$REPO/tree/$T1_SEED ($LOCAL)"
