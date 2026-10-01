# Arden Capital Markets — engineering rules Devin must follow (synthetic estate; upload as Knowledge, scope "when working in any Arden repository")

- **Internal artifact mirror only.** Maven/npm/pip resolve through `artifactory.ardencm.internal` (`common-java-bom/pom.xml` `<repositories>`; `ci-shared-workflows/.github/workflows/java-build.yml`). Never add a public repository to a build file.
- **Approved base images** are the ones referenced by `ci-shared-workflows/.github/workflows/*.yml`. Do not pin an image or runner version that is not used there.
- **No transitive bumps without owner sign-off.** A dependency bump that changes a *transitive* version another repository consumes (e.g. anything under `common-java-bom`) needs the BOM owner (`@ardencm/platform-eng`) named as reviewer and the reason stated in the PR.
- **Wire formats are contracts.** FIX tag 64/75 stay `LocalMktDate` (`yyyyMMdd`); ISO 20022 field mappings live in `iso20022-fix-messages/mapping/`. If a library upgrade changes a wire format, pin the compatible line and add a contract test rather than adapting consumers.
- **CODEOWNERS routes review.** Every PR names the CODEOWNERS team(s) for the files changed. Do not @-mention individuals found via git blame.
- **Nothing merges or deploys from a Devin session.** Open PRs; rollout is `release-bot` on a human approval.
- **Commit messages** contain `feature` or `bug`. **PR descriptions** end with the line `Devin-Org: engineering`.
- **Secrets**: rotate-then-remove. A committed credential is a rotation ticket for `@ardencm/security` first; the code change replaces it with an environment/vault reference and never with a new literal.
