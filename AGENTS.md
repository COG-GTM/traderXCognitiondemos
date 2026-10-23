# Agent notes for this repository

- `arden/` holds part of the Arden Capital Markets estate (a synthetic bank estate built for demos). The other estate repos live under `arden/` in `COG-GTM/Securities-Trade-Processing-System` and `COG-GTM/Springboot-BankApp` on branch `devin/1790864747-arden-finserv-estate`. Map: `arden/finserv-estate/scripts/repo-map.tsv`.
- Arden's internal Artifactory (`artifactory.ardencm.internal`) is a fixture and is not reachable. Stand it in once per machine:
  1. `sudo apt-get install -y maven` if `mvn` is missing, and write `~/.m2/settings.xml` with a mirror of `central` at `https://maven-central.storage-download.googleapis.com/maven2/` (Maven Central returns HTTP 429 to these VMs). Never add a public repository to a build file.
  2. `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh` (about 1 minute). It installs Arden's internal releases (`core-dates` 1.4.0, 1.4.1, 1.5.0; `common-java-bom`) into `~/.m2` and seeds `~/finserv-workspace/<repo>` with each repo's git history.
- Open PRs on the COG-GTM repo that hosts the estate repo you changed (`arden/finserv-estate/scripts/publish-branch.sh`). Never merge, deploy, or post to a real Slack channel.
- `client-portal` runs with `npm start` on port 3200 (`ARDENFEED_BATCH=fixtures/<batch>.json` selects a feed batch). Tests: `npm test`.
- Start long-running processes with `setsid`, not plain `nohup`.
