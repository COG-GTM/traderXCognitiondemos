# Demo 4 talk track: audit readiness and traceability

Use this presenter script beat by beat. Demo 4 maps five controls, shows one code fix, one recorded risk decision, and the named owners for evidence gaps.

## Beats

### 0. Setup

**Run:** `demo/setup-demo.sh` once on a fresh Linux machine. It took 3.46 seconds in the verification environment. A fresh machine may take longer when dependencies need installing.

**Show on screen:** The setup terminal ending with `READY: Demo 4, audit readiness and traceability (FINOS CCC.Core v2025.10)`.

**Say:** "The setup checks Python dependencies, resets the source snapshot, runs the audit tests, and smoke-checks the evidence pack. Maven, JDK, and Node are not used."

### 1. The ask

**Run:** No command.

**Show on screen:** Keep the terminal ready.

**Say:** "Your auditor just asked: show me every platform control, and who changed what, why, and who approved it. Devin answers with one run."

### 2. Map the controls

**Run:** `demo/run-demo4.sh`, 1.28 seconds in the verification environment.

**Show on screen:** The terminal summary with five controls: one FIX, one RISK, one PASS, and two GAP.

**Say:** "Devin maps the platform and shared CI configuration to the stable FINOS CCC.Core catalog, then builds a linked evidence pack."

### 3. Open the control map

**Run:** Open `demo/out/audit/control-map.html`.

**Show on screen:** The control map and its Evidence column. Click an Evidence link to show the real GitHub source line.

**Say:** "The wow moment is that every row points to evidence. The source line, pull request, risk memo, and named owner questions are all one click away."

### 4. CN13 FIX

**Run:** Click the CN13 PR #327 evidence link.

**Show on screen:** The token definitions, PR #327, and the commit showing the 12 Artifactory tokens.

**Say:** "Devin found 12 tokens that never expire and opened a fix. The change is not approved yet. Humans approve."

### 5. The ONE hard case: CN05 risk

**Run:** Open `demo/out/audit/evidence-pack/risk-decision-CN05.html`.

**Show on screen:** The PLAT-771 open-ingress exception and the risk decision memo.

**Say:** "This is the one hard case. Open ingress has a PLAT-771 exception. Devin wrote a risk memo and changed no code because the exception needs a human risk decision."

### 6. GAP rows

**Run:** Return to the control map and point to CN04 and CN09.

**Show on screen:** The named owner, due date, and searched-tree evidence links for each GAP.

**Say:** "These controls need account-level evidence outside the scanned repositories. Devin names the owner and due date, Devin does not guess."

### 7. Traceability

**Run:** Open `demo/out/audit/traceability.html`.

**Show on screen:** The ticket, requester, Devin commands, commit, CI status, approval, and deployment rows.

**Say:** "This audit-log row is a recorded snapshot fixture because the demo org's API token lacks ViewAccountAuditLogs. Run with `--live` to use the real export. If the live export is unavailable, Devin falls back to the recorded snapshot and prints one explanatory line."

### 8. Close

**Run:** No command.

**Show on screen:** Leave the control map open.

**Say:** "The auditor gets a control map, a safe decision boundary, named owners for the remaining evidence, and a traceable change story from one run."

## Expected numbers

| Measure | Expected |
|---|---:|
| FIX | 1 |
| RISK | 1 |
| PASS | 1 |
| GAP | 2 |
| Artifactory token definitions | 12 |
| Audit tests | 20 |

## Timings

| Step | Measured time |
|---|---:|
| `demo/setup-demo.sh` | 3.46 seconds in the verification environment |
| `demo/run-demo4.sh` | 1.28 seconds in the verification environment |
| `demo/audit/reset.sh` | 2.95 seconds in the verification environment |

**Reset:** `demo/audit/reset.sh` (about 2 s).
