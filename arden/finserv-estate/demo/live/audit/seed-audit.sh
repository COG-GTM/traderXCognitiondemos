#!/usr/bin/env bash
# demo_audit: seed the FINOS CCC audit situation a presenter hands to Devin.
#   1. Checks the platform controls in Springboot-BankApp are at the BEFORE state (CN13 tokens never expire, PLAT-771 ingress).
#   2. Closes CN13/CCC PRs left in Springboot-BankApp by earlier LIVE runs (never the protected Demo 1 / replay PRs).
#   3. Re-creates demo/arden-live-before with the shared seed-situation.sh.
#   4. Prints READY with the seed URL, prompt 1 and the fallback.
#
#   arden/finserv-estate/demo/live/audit/seed-audit.sh
#   DEMO_AUDIT_DRY_RUN=1 .../seed-audit.sh        # check and list only: closes nothing, pushes nothing
#   DEMO_AUDIT_KEEP_PRS="333" .../seed-audit.sh    # extra Springboot PR numbers to keep open
set -eEuo pipefail

SB_REPO="${DEMO_AUDIT_SB_REPO:-COG-GTM/Springboot-BankApp}"
ESTATE_BRANCH=devin/1790864747-arden-finserv-estate
PROTECTED_PRS="316 317 318 319 320 327 ${DEMO_AUDIT_KEEP_PRS:-}"
DRY_RUN="${DEMO_AUDIT_DRY_RUN:-}"
HERE=$(cd "$(dirname "$0")" && pwd)
CURRENT_STEP="initializing"

trap 'status=$?; printf "SEED FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

step "Tools"
for t in git gh python3 curl; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

step "Platform controls in $SB_REPO@$ESTATE_BRANCH are at the BEFORE state"
raw() { gh api -H "Accept: application/vnd.github.raw" "repos/$SB_REPO/contents/$1?ref=$ESTATE_BRANCH"; }
TOKENS=$(raw arden/platform-terraform/artifactory/ci-tokens.tf) || fail "cannot read ci-tokens.tf"
MAIN=$(raw arden/platform-terraform/artifactory/main.tf) || fail "cannot read main.tf"
grep -Eq '^\s*expires_in\s*=\s*0\b' <<<"$TOKENS" || fail "CN13 seed missing: ci-tokens.tf no longer has expires_in = 0"
grep -Eq 'expires_in_seconds\s*=\s*0\b' <<<"$MAIN" || fail "CN13 seed missing: main.tf no longer has expires_in_seconds = 0"
grep -q 'PLAT-771' <<<"$MAIN" || fail "CN05 seed missing: PLAT-771 exception comment not in main.tf"
grep -q '0.0.0.0/0' <<<"$MAIN" || fail "CN05 seed missing: 0.0.0.0/0 ingress not in main.tf"
echo "    CN13 BEFORE (expires_in = 0), CN05 BEFORE (0.0.0.0/0 under PLAT-771)"

step "Close CN13/CCC PRs left in $SB_REPO by earlier live runs"
gh pr list -R "$SB_REPO" --base "$ESTATE_BRANCH" --state open --limit 100 \
   --json number,headRefName,title --jq '.[] | "\(.number)\t\(.headRefName)\t\(.title)"' |
while IFS=$'\t' read -r num head title; do
  [[ " $PROTECTED_PRS " == *" $num "* ]] && { echo "    keep  #$num (protected) $title"; continue; }
  [[ "$head" == devin/* ]] || continue
  grep -Eqi 'CN13|CCC\.Core|ccc-cn|token[- ]lifetime' <<<"$head $title" || continue
  if [[ -n "$DRY_RUN" ]]; then echo "    would close #$num ($head) $title"; continue; fi
  gh pr close -R "$SB_REPO" "$num" --delete-branch \
     --comment "Closed by demo/live/audit/seed-audit.sh: reseeding the demo_audit situation for a new live run." >/dev/null
  echo "    closed #$num ($head)"
done

if [[ -n "$DRY_RUN" ]]; then
  step "Dry run: skipping seed-situation.sh"
  SEED_LINE="(dry run, not reseeded)"
else
  step "Re-create the seed branch"
  "$HERE/../seed-situation.sh"
  SEED_LINE=""
fi

SEED_BRANCH="${DEMO_ARDEN_SEED_BRANCH:-demo/arden-live-before}"
SEED_REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
PROMPT=$(python3 - "$HERE/PROMPTS.md" <<'PY'
import re, sys
text = open(sys.argv[1]).read()
m = re.search(r"<!-- PROMPT1 -->\s*```text\n(.*?)\n```", text, re.S)
sys.exit("PROMPT1 block not found in PROMPTS.md") if not m else print(m.group(1))
PY
) || fail "read prompt 1 from PROMPTS.md"

cat <<BANNER

READY: demo_audit (FINOS CCC.Core v2025.10 audit readiness) $SEED_LINE
  Seed branch: https://github.com/$SEED_REPO/tree/$SEED_BRANCH
  Fix PRs go:  https://github.com/$SB_REPO/pulls?q=is%3Apr+is%3Aopen+base%3A$ESTATE_BRANCH
  Prompt 1 (paste into a new Devin session):
$(sed 's/^/    /' <<<"$PROMPT")
  Fallback (replay of traderX PR #119, no Devin needed):
    git fetch origin devin/fs-audit-1791848400-ccc-traceability && git checkout devin/fs-audit-1791848400-ccc-traceability
    arden/finserv-estate/demo/setup-demo4.sh && arden/finserv-estate/demo/run-demo4.sh
BANNER
