#!/usr/bin/env bash
# Turn a demo PR artifact (demo/out/**/prs/<repo>/*.patch) into a real branch on the COG-GTM repo that hosts
# that estate repo (scripts/repo-map.tsv), ready for a PR:
#
#   scripts/publish-branch.sh <estate-repo> <branch> <patch> [<patch>...]
#   e.g. scripts/publish-branch.sh settlement-instruction-service \
#          devin/$(date +%s)-sec-2026-0912-core-dates-pin demo/out/scanner/prs/settlement-instruction-service/change.patch
#
# The patch was produced in the seeded workspace (repo root = estate repo), so it is applied with
# --directory=<subdir>. Base = the mapped ref (feature branch until merged; FS_SOURCE_REF=main after).
# Pushes the branch; does NOT open the PR (do that with the PR tooling so the body carries the PR.md content
# and ends with `Devin-Org: engineering`). Never pushes to the base branch. FS_PUBLISH_DRY_RUN=1 skips the push.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/lib.sh"
[[ $# -ge 3 ]] || die "usage: $0 <estate-repo> <branch> <patch> [<patch>...]"
repo="$1" branch="$2"; shift 2
gh="$(repo_github "$repo")" ref="$(repo_ref "$repo")" subdir="$(repo_subdir "$repo")"
dir="$(repo_clone_dir "$gh")"
case "$branch" in main|master|DevOps|"$ref") die "refusing to publish onto base branch $branch";; esac
[[ -d "$dir/.git" ]] || sources_sync
patches=(); for p in "$@"; do patches+=("$(cd "$(dirname "$p")" && pwd)/$(basename "$p")"); done

cd "$dir"
[[ -z "$(git status --porcelain)" ]] || die "$dir has local changes; refusing to switch branches"
git fetch -q origin "$ref"
git checkout -q -B "$branch" "origin/$ref"
for p in "${patches[@]}"; do
  log "applying $(basename "$p") under $subdir/"
  if ! git am -q --3way --directory="$subdir" "$p"; then
    git am --abort || true
    die "patch did not apply cleanly to origin/$ref:$subdir — resolve by hand on branch $branch (see PR.md next to the patch)"
  fi
done
git log --oneline "origin/$ref..$branch"
if [[ "${FS_PUBLISH_DRY_RUN:-0}" == "1" ]]; then log "dry run: not pushing $branch to $gh"; exit 0; fi
git push -q -u origin "$branch"
log "pushed https://github.com/$gh/tree/$branch — open the PR from PR.md (body must end with 'Devin-Org: engineering')"
