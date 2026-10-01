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
# If a patch does not apply (core-dates: the seeded workspace history is ahead of the pristine tree, so the patch's
# base blobs are unknown here) the sub-tree is synced to the workspace commit with that subject instead, so the PR
# shows the workspace state the demo verified.
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
patches=(); for p in "$@"; do [[ -f "$p" ]] || die "patch not found: $p"; patches+=("$(realpath "$p")"); done

cd "$dir"
[[ -z "$(git status --porcelain)" ]] || die "$dir has local changes; refusing to switch branches"
git fetch -q origin "$ref"
git checkout -q -B "$branch" "origin/$ref"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
sync_from_workspace() {  # sync_from_workspace <patch>: subtree := workspace commit whose subject matches the patch
  local subj sha tmp
  subj="$(git mailinfo /dev/null /dev/null < "$1" | sed -n 's/^Subject: //p')"   # decodes RFC 2047 subjects
  [[ -d "$WS/$repo/.git" ]] || die "patch did not apply and no workspace repo at $WS/$repo"
  sha="$(git -C "$WS/$repo" log --all --format='%H%x09%s' | awk -F'\t' -v s="$subj" '$2 == s { print $1; exit }')"
  [[ -n "$sha" ]] || die "patch did not apply and no workspace commit titled '$subj' in $WS/$repo"
  warn "patch base unknown here (seeded history); syncing $subdir/ to workspace commit ${sha:0:7} instead"
  tmp="$(mktemp -d)"; git -C "$WS/$repo" archive "$sha" | tar -x -C "$tmp"
  rsync -a --delete --exclude .git --exclude target/ --exclude build/ --exclude dist/ --exclude node_modules/ \
    --exclude __pycache__/ --exclude .pytest_cache/ --exclude .seed/ "$tmp/" "$subdir/"
  rm -rf "$tmp"
  git add -A -- "$subdir"
  git -C "$WS/$repo" log -1 --format=%B "$sha" | git commit -q -F -
}
for p in "${patches[@]}"; do
  log "applying $(basename "$p") under $subdir/"
  if ! git am -q --3way --directory="$subdir" "$p" 2>/dev/null; then
    git am --abort 2>/dev/null || true
    sync_from_workspace "$p"
  fi
done
git log --oneline "origin/$ref..$branch"
if [[ "${FS_PUBLISH_DRY_RUN:-0}" == "1" ]]; then log "dry run: not pushing $branch to $gh"; exit 0; fi
git push -q -u origin "$branch"
log "pushed https://github.com/$gh/tree/$branch — open the PR from PR.md (body must end with 'Devin-Org: engineering')"
