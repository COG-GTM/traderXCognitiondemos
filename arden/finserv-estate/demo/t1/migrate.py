#!/usr/bin/env python3
"""Demo 2, step 2 — execute tranche 1 of the T+1 migration deterministically in the workspace, one branch + PR
artifact per repo, with repo tests and consumer contract tests run at every step.

    python3 demo/t1/migrate.py --tranche 1

Tranche 1 = the instruction-generation path: core-dates (date-gated default cycle) → settlement-instruction-service
(cycle resolved from trade date; venue pins removed) → iso20022-fix-messages (mapping + golden sample).
Before touching core-dates the executor builds and stashes the *baseline* CLI jar so demo/t1/parity.py can replay
the golden file through old and new code.  In a live run each of these three PRs is a child Devin session.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import shutil
import sys
import textwrap

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from estatelib import OUT, WORKSPACE, git, java_env, log, now_iso, owners_for, run, target_line, write_json  # noqa: E402

GO_LIVE = "2027-10-11"
NEW_CORE_DATES = "1.4.2"
PROGRAMME = "SETTLE-4471"

# ----------------------------------------------------------------------------------------------------------------
# core-dates
# ----------------------------------------------------------------------------------------------------------------
SETTLEMENT_CYCLE_JAVA = textwrap.dedent(f"""\
    package com.ardencm.posttrade.dates;

    import java.time.LocalDate;

    /**
     * Standard settlement cycle for cash equities and fixed income.
     *
     * <p>US and Canada moved to T+1 on 28 May 2024. EU, UK and Switzerland move on
     * {{@link #EU_UK_CH_T1_GO_LIVE 11 October 2027}}. The default cycle is therefore a function of the trade date:
     * trades dated before go-live settle T+2, trades dated on or after it settle T+1. Callers must pass the trade
     * date; the no-argument forms are kept only so existing binaries compile and are deprecated.
     */
    public enum SettlementCycle {{
        T_PLUS_0(0),
        T_PLUS_1(1),
        T_PLUS_2(2),
        T_PLUS_3(3);

        /** First trade date settling T+1 in EU/UK/CH markets ({PROGRAMME}). */
        public static final LocalDate EU_UK_CH_T1_GO_LIVE = LocalDate.of(2027, 10, 11);

        private final int businessDays;

        SettlementCycle(int businessDays) {{
            this.businessDays = businessDays;
        }}

        public int businessDays() {{
            return businessDays;
        }}

        /** Default cycle for EU/UK/CH cash markets for a trade executed on {{@code tradeDate}}. */
        public static SettlementCycle standard(LocalDate tradeDate) {{
            return tradeDate.isBefore(EU_UK_CH_T1_GO_LIVE) ? T_PLUS_2 : T_PLUS_1;
        }}

        /**
         * @deprecated the standard cycle depends on the trade date since {PROGRAMME}; use {{@link #standard(LocalDate)}}.
         *     Returns the pre-go-live value so existing callers keep their behaviour until migrated (tranche 2).
         */
        @Deprecated
        public static SettlementCycle standard() {{
            return T_PLUS_2;
        }}

        /** Resolves the standard cycle for a market identifier code (ISO 10383 MIC) and trade date. */
        public static SettlementCycle forMarket(String mic, LocalDate tradeDate) {{
            switch (mic) {{
                case "XNYS":
                case "XNAS":
                case "XTSE":
                    return T_PLUS_1;
                default:
                    return standard(tradeDate);
            }}
        }}

        /** @deprecated use {{@link #forMarket(String, LocalDate)}}. */
        @Deprecated
        public static SettlementCycle forMarket(String mic) {{
            switch (mic) {{
                case "XNYS":
                case "XNAS":
                case "XTSE":
                    return T_PLUS_1;
                default:
                    return standard();
            }}
        }}
    }}
""")

SETTLEMENT_DATES_TEST_JAVA = textwrap.dedent("""\
    package com.ardencm.posttrade.dates;

    import static org.junit.jupiter.api.Assertions.assertEquals;

    import java.time.LocalDate;
    import org.junit.jupiter.api.Nested;
    import org.junit.jupiter.api.Test;

    class SettlementDatesTest {

        @Nested
        class BeforeGoLive {
            @Test
            void londonTradeSettlesTwoBusinessDaysLater() {
                LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 6), "XLON");
                assertEquals(LocalDate.of(2027, 10, 8), sd);
            }

            @Test
            void lastTPlusTwoTradeDateSettlesOnTheFirstTPlusOneSettlementDate() {
                // Fri 8 Oct 2027 (T+2) and Mon 11 Oct 2027 (T+1) both settle Tue 12 Oct: the double-settlement day.
                assertEquals(LocalDate.of(2027, 10, 12), SettlementDates.settlementDate(LocalDate.of(2027, 10, 8), "XLON"));
                assertEquals(LocalDate.of(2027, 10, 12), SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XLON"));
            }

            @Test
            void londonBankHolidayIsSkipped() {
                // Friday 27 Aug 2027 -> Mon 30 Aug is a bank holiday -> Wed 1 Sep
                LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 8, 27), "XLON");
                assertEquals(LocalDate.of(2027, 9, 1), sd);
            }
        }

        @Nested
        class FromGoLive {
            @Test
            void londonTradeSettlesNextBusinessDay() {
                LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 11), "XLON");
                assertEquals(LocalDate.of(2027, 10, 12), sd);
            }

            @Test
            void weekendIsSkipped() {
                // Friday trade -> Monday settlement under T+1
                LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 15), "XLON");
                assertEquals(LocalDate.of(2027, 10, 18), sd);
            }

            @Test
            void fixTag63ForStandardCycleIsNextDay() {
                assertEquals("2", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 11))));
                assertEquals("3", FixDates.tag63(SettlementCycle.standard(LocalDate.of(2027, 10, 8))));
            }
        }

        @Test
        void usMarketsAlreadyOnTPlusOne() {
            LocalDate sd = SettlementDates.settlementDate(LocalDate.of(2027, 10, 6), "XNYS");
            assertEquals(LocalDate.of(2027, 10, 7), sd);
        }

        @Test
        void deprecatedNoArgFormsKeepPreGoLiveBehaviour() {
            assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.standard());
            assertEquals(SettlementCycle.T_PLUS_2, SettlementCycle.forMarket("XLON"));
        }
    }
""")

INSTRUCTION_BUILDER_TEST_JAVA = textwrap.dedent("""\
    package com.ardencm.settlements.instruction;

    import com.ardencm.posttrade.dates.SettlementCycle;
    import org.junit.jupiter.api.Test;

    import java.time.LocalDate;
    import java.util.Map;

    import static org.junit.jupiter.api.Assertions.assertEquals;
    import static org.junit.jupiter.api.Assertions.assertTrue;

    class InstructionBuilderTest {
        /** Mirrors application.yml: North America pinned, EU/UK/CH fall back to the date-gated default. */
        private InstructionBuilder builder() {
            SettlementCycleConfig cfg = new SettlementCycleConfig();
            cfg.setCycles(Map.of("XNYS", SettlementCycle.T_PLUS_1, "XNAS", SettlementCycle.T_PLUS_1, "XTSE", SettlementCycle.T_PLUS_1));
            return new InstructionBuilder(cfg);
        }

        @Test
        void londonInstructionBeforeGoLiveSettlesTPlusTwo() {
            Instruction i = builder().build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 6));
            assertEquals(LocalDate.of(2027, 10, 8), i.settlementDate());
            assertEquals("20271008", i.fixSettlDate());
            assertEquals("3", i.fixSettlType());
        }

        @Test
        void londonInstructionFromGoLiveSettlesTPlusOne() {
            Instruction i = builder().build("T-2", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
            assertEquals(LocalDate.of(2027, 10, 12), i.settlementDate());
            assertEquals("20271012", i.fixSettlDate());
            assertEquals("2", i.fixSettlType());
        }

        @Test
        void explicitVenuePinStillWins() {
            SettlementCycleConfig cfg = new SettlementCycleConfig();
            cfg.setCycles(Map.of("XLON", SettlementCycle.T_PLUS_2));
            Instruction i = new InstructionBuilder(cfg).build("T-3", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11));
            assertEquals(LocalDate.of(2027, 10, 13), i.settlementDate());
        }

        @Test
        void fixMessageCarriesLocalMktDate() {
            InstructionBuilder b = builder();
            String fix = b.toFix(b.build("T-1", "GB0002634946", "XLON", LocalDate.of(2027, 10, 11)));
            assertTrue(fix.contains("\\u000164=20271012\\u0001"), fix);
            assertTrue(fix.contains("\\u000163=2\\u0001"), fix);
        }
    }
""")


def java_summary(repo: pathlib.Path) -> str:
    import xml.etree.ElementTree as ET
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed = []
    for xml in sorted((repo / "target/surefire-reports").glob("TEST-*.xml")):
        root = ET.parse(xml).getroot()
        for k in totals:
            totals[k] += int(root.get(k, "0"))
        for tc in root.iter("testcase"):
            if any(c.tag in ("failure", "error") for c in tc):
                failed.append(f"{tc.get('classname', '').rsplit('.', 1)[-1]}.{tc.get('name')}")
    s = f"Tests run: {totals['tests']}, Failures: {totals['failures']}, Errors: {totals['errors']}, Skipped: {totals['skipped']}"
    return s + (" — FAILED: " + ", ".join(failed) if failed else "")


def mvn(repo: pathlib.Path, *goals: str) -> tuple[bool, str]:
    shutil.rmtree(repo / "target/surefire-reports", ignore_errors=True)
    r = run(["mvn", "-q", "-o", *goals], repo, env=java_env())
    if not r.ok and "offline mode" in r.stdout:
        r = run(["mvn", "-q", *goals], repo, env=java_env())
    if (repo / "target/surefire-reports").exists():
        return r.ok, java_summary(repo)
    return r.ok, "BUILD " + ("OK" if r.ok else "FAILED: " + (r.stdout.strip().splitlines() or [""])[0][:160])


def pytest_(repo: pathlib.Path) -> tuple[bool, str]:
    r = run([sys.executable, "-m", "pytest", "-q"], repo)
    last = [l for l in r.stdout.strip().splitlines() if "passed" in l or "failed" in l or "error" in l]
    return r.ok, (last[-1] if last else r.stdout.strip()[-160:])


def set_core_dates_version(pom: pathlib.Path, version: str) -> None:
    text = pom.read_text()
    block = re.compile(r"(<artifactId>core-dates</artifactId>\n)(\s*<version>[^<]+</version>\n)?")
    text = block.sub(lambda m: m.group(1) + m.group(1).replace("<artifactId>core-dates</artifactId>", f"<version>{version}</version>"), text, count=1)
    pom.write_text(text)


def sub(path: pathlib.Path, old: str, new: str, count: int = 1) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"expected text not found in {path}: {old[:60]!r}")
    path.write_text(text.replace(old, new, count))


# ----------------------------------------------------------------------------------------------------------------
# per-repo steps: each returns a list of attempts (strategy, changes, tests_ok, test_summary[, reason])
# ----------------------------------------------------------------------------------------------------------------
def bom_core_dates_version() -> str:
    bom = (WORKSPACE / "common-java-bom/pom.xml").read_text()
    m = re.search(r"<core-dates\.version>([^<]+)</core-dates\.version>", bom)
    return m.group(1) if m else "1.4.0"


def apply_date_gate(repo: pathlib.Path, version: str) -> list[str]:
    src = repo / "src/main/java/com/ardencm/posttrade/dates"
    (src / "SettlementCycle.java").write_text(SETTLEMENT_CYCLE_JAVA)
    sub(src / "SettlementDates.java",
        "return settlementDate(tradeDate, mic, SettlementCycle.forMarket(mic));",
        "return settlementDate(tradeDate, mic, SettlementCycle.forMarket(mic, tradeDate));")
    sub(src / "SettlementDateCli.java",
        "SettlementCycle cycle = override != null ? override : SettlementCycle.forMarket(mic);",
        "SettlementCycle cycle = override != null ? override : SettlementCycle.forMarket(mic, tradeDate);")
    (repo / "src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java").write_text(SETTLEMENT_DATES_TEST_JAVA)
    pom = repo / "pom.xml"
    pom.write_text(re.sub(r"<version>[^<]+</version>", f"<version>{version}</version>", pom.read_text(), count=1))
    readme = repo / "README.md"
    readme.write_text(re.sub(r"core-dates-[\d.]+-cli\.jar", f"core-dates-{version}-cli.jar", readme.read_text()))
    sub(repo / "CHANGELOG.md", "# Changelog\n",
        f"# Changelog\n\n## {version} — {PROGRAMME} T+1 (EU/UK/CH)\n"
        f"- `SettlementCycle.standard(LocalDate)` / `forMarket(mic, LocalDate)`: T+2 for trade dates before {GO_LIVE}, T+1 from {GO_LIVE}.\n"
        "- `SettlementDates.settlementDate(tradeDate, mic)` and the CLI resolve the cycle from the trade date.\n"
        "- No-arg `standard()` / `forMarket(mic)` deprecated; still return the pre-go-live cycle so unmigrated callers do not change behaviour silently.\n"
        "- No wire-format changes: tag 64 stays LocalMktDate `yyyyMMdd`; tag 63 follows the resolved cycle.\n")
    return ["pom.xml", "CHANGELOG.md", "README.md", "src/main/java/.../SettlementCycle.java", "src/main/java/.../SettlementDates.java",
            "src/main/java/.../SettlementDateCli.java", "src/test/java/.../SettlementDatesTest.java"]


def consumer_contract_check(version: str) -> tuple[bool, str]:
    """Run the downstream consumer's *ContractTest against a candidate core-dates release, then put its pom back."""
    sis = WORKSPACE / "settlement-instruction-service"
    set_core_dates_version(sis / "pom.xml", version)
    ok, summary = mvn(sis, "test", "-Dtest=*ContractTest", "-Dsurefire.failIfNoSpecifiedTests=false")
    git(sis, "checkout", "--", "pom.xml")
    return ok, f"settlement-instruction-service CustodyGatewayContractTest vs core-dates {version}: {summary}"


def migrate_core_dates(repo: pathlib.Path) -> tuple[list[dict], str]:
    attempts = []
    branch = "t1/tranche-1-core-dates"
    # baseline = the release consumers actually run (pinned by common-java-bom), stashed for the parity harness
    base_v = bom_core_dates_version()
    base_jar = pathlib.Path.home() / f".m2/repository/com/ardencm/posttrade/core-dates/{base_v}/core-dates-{base_v}-cli.jar"
    (OUT / "t1").mkdir(parents=True, exist_ok=True)
    shutil.copy(base_jar, OUT / "t1/baseline-cli.jar")
    attempts.append({"strategy": f"baseline: core-dates {base_v} (common-java-bom pin) CLI stashed for the parity harness", "changes": [],
                     "tests_ok": True, "test_summary": f"baseline-cli.jar = core-dates {base_v}"})

    # attempt 1: release from main
    git(repo, "checkout", "-q", "main")
    git(repo, "branch", "-D", branch + "-from-main")
    git(repo, "checkout", "-q", "-b", branch + "-from-main")
    changes = apply_date_gate(repo, "1.6.0")
    ok1, s1 = mvn(repo, "install")
    okc, sc = consumer_contract_check("1.6.0") if ok1 else (False, "not run")
    git(repo, "add", "-A")
    git(repo, "commit", "-q", "-m", f"feature: {PROGRAMME} T+1 date gate on main (candidate 1.6.0)")
    attempts.append({"strategy": "date-gate the default cycle on main → core-dates 1.6.0, then run downstream consumer contract tests",
                     "changes": changes, "tests_ok": ok1 and okc, "test_summary": f"core-dates: {s1} · {sc}",
                     "reason": "main carries 1.5.0's FixDates change (tag 64/75 emit ISO-8601). Any release cut from main breaks the custody-gateway "
                               "FIX contract, so the T+1 change cannot ship from main until that is resolved (out of scope for SETTLE-4471)."})
    if ok1 and okc:
        shutil.copy(repo / "target/core-dates-1.6.0-cli.jar", OUT / "t1/migrated-cli.jar")
        return attempts, branch + "-from-main"

    # attempt 2 (deviation): cut the release from release/1.4 — the line consumers run — with only the date gate on top
    git(repo, "checkout", "-q", "release/1.4")
    git(repo, "branch", "-D", branch)
    git(repo, "checkout", "-q", "-b", branch)
    changes = apply_date_gate(repo, NEW_CORE_DATES)
    ok2, s2 = mvn(repo, "install")
    okc2, sc2 = consumer_contract_check(NEW_CORE_DATES) if ok2 else (False, "not run")
    attempts.append({"strategy": f"deviation: branch from release/1.4 (what consumers run) and release core-dates {NEW_CORE_DATES} with the date gate only",
                     "changes": changes, "tests_ok": ok2 and okc2, "test_summary": f"core-dates: {s2} · {sc2}",
                     "reason": "Same source change, different base: release/1.4 keeps LocalMktDate so the consumer contract holds. Forward-port to main is "
                               "required and is listed as a follow-up; common-java-bom must move its pin to " + NEW_CORE_DATES + " (platform-eng)."})
    new_cli = repo / f"target/core-dates-{NEW_CORE_DATES}-cli.jar"
    if new_cli.exists():
        shutil.copy(new_cli, OUT / "t1/migrated-cli.jar")
    return attempts, branch


def migrate_settlement_instruction_service(repo: pathlib.Path) -> tuple[list[dict], str]:
    branch = "t1/tranche-1-settlement-instruction-service"
    git(repo, "checkout", "-q", "main")
    git(repo, "branch", "-D", branch)
    git(repo, "checkout", "-q", "-b", branch)
    attempts = []
    ok0, base = mvn(repo, "test")
    attempts.append({"strategy": "baseline: repo tests + CustodyGatewayContractTest before any change", "changes": [], "tests_ok": ok0, "test_summary": base})
    set_core_dates_version(repo / "pom.xml", NEW_CORE_DATES)
    pkg = repo / "src/main/java/com/ardencm/settlements/instruction"
    sub(pkg / "SettlementCycleConfig.java", "import java.util.HashMap;", "import java.time.LocalDate;\nimport java.util.HashMap;")
    sub(pkg / "SettlementCycleConfig.java",
        "    public SettlementCycle cycleFor(String mic) {\n        return cycles.getOrDefault(mic, SettlementCycle.standard());\n    }",
        "    /** Explicit venue pins win; everything else follows the date-gated market default (T+2 → T+1 on 11 Oct 2027). */\n"
        "    public SettlementCycle cycleFor(String mic, LocalDate tradeDate) {\n        return cycles.getOrDefault(mic, SettlementCycle.standard(tradeDate));\n    }")
    sub(pkg / "SettlementCycleConfig.java",
        " * Per-venue settlement cycle overrides. Anything not listed falls back to {@link SettlementCycle#standard()}.",
        " * Per-venue settlement cycle overrides. Anything not listed falls back to {@link SettlementCycle#standard(LocalDate)}.")
    sub(pkg / "InstructionBuilder.java", "SettlementCycle cycle = config.cycleFor(mic);", "SettlementCycle cycle = config.cycleFor(mic, tradeDate);")
    sub(repo / "src/main/resources/application.yml",
        "    # EU/UK/CH venues: regular way T+2 (CSDR art. 5). Migration to T+1 scheduled 11 Oct 2027 — see SETTLE-4471.\n"
        "    XLON: T_PLUS_2\n    XPAR: T_PLUS_2\n    XETR: T_PLUS_2\n    XSWX: T_PLUS_2\n",
        "    # EU/UK/CH venues are intentionally NOT pinned: core-dates resolves T+2 / T+1 from the trade date\n"
        f"    # (go-live {GO_LIVE}, SETTLE-4471). Add a pin here only for a venue that deviates from the market default.\n")
    (repo / "src/test/java/com/ardencm/settlements/instruction/InstructionBuilderTest.java").write_text(INSTRUCTION_BUILDER_TEST_JAVA)
    ok, summary = mvn(repo, "test")
    attempts.append({"strategy": f"core-dates {NEW_CORE_DATES}; cycleFor(mic, tradeDate); drop EU/UK/CH T_PLUS_2 pins; split tests into pre/post go-live",
                     "changes": ["pom.xml", "src/main/resources/application.yml", "src/main/java/.../SettlementCycleConfig.java",
                                 "src/main/java/.../InstructionBuilder.java", "src/test/java/.../InstructionBuilderTest.java"],
                     "tests_ok": ok, "test_summary": summary,
                     "reason": "CustodyGatewayContractTest (tag 64 = yyyyMMdd) is unchanged and included in the run."})
    return attempts, branch


def migrate_iso20022(repo: pathlib.Path) -> tuple[list[dict], str]:
    branch = "t1/tranche-1-iso20022-fix-messages"
    git(repo, "checkout", "-q", "main")
    git(repo, "branch", "-D", branch)
    git(repo, "checkout", "-q", "-b", branch)
    attempts = []
    ok0, base = pytest_(repo)
    attempts.append({"strategy": "baseline: python -m pytest + validate.py", "changes": [], "tests_ok": ok0, "test_summary": base})
    m = repo / "mapping/settlement-cycle.yaml"
    sub(m, "default_cycle: T+2\ndefault_fix_settl_type: \"3\"",
        f"# EU/UK/CH regular way moved to T+1 on {GO_LIVE} (SETTLE-4471). Messages for trade dates before that\n"
        "# must be generated from the pre-migration mapping (archive/settlement-cycle-pre-t1.yaml).\n"
        f"effective_from: \"{GO_LIVE}\"\ndefault_cycle: T+1\ndefault_fix_settl_type: \"2\"")
    for mic in ("XLON", "XPAR", "XETR", "XSWX"):
        sub(m, f"  {mic}: {{ cycle: T+2, fix_settl_type: \"3\" }}", f"  {mic}: {{ cycle: T+1, fix_settl_type: \"2\" }}")
    (repo / "mapping/archive").mkdir(exist_ok=True)
    git(repo, "show", "HEAD:mapping/settlement-cycle.yaml")
    (repo / "mapping/archive/settlement-cycle-pre-t1.yaml").write_text(git(repo, "show", "HEAD:mapping/settlement-cycle.yaml").stdout)
    sample = repo / "samples/fix/AE-xlon-regular-way.fix"
    (repo / "samples/fix/archive").mkdir(exist_ok=True)
    shutil.copy(sample, repo / "samples/fix/archive/AE-xlon-regular-way-pre-t1.fix")
    sub(sample, "|63=3|64=20271013|", "|63=2|64=20271012|")
    sub(repo / "README.md", "outbound FIX 4.4 and ISO 20022 sese.023.",
        f"outbound FIX 4.4 and ISO 20022 sese.023. EU/UK/CH regular way is T+1 from {GO_LIVE} (tag 63 = 2); the pre-migration\n"
        "mapping and sample are kept under `mapping/archive/` and `samples/fix/archive/` for back-dated trades.")
    ok, summary = pytest_(repo)
    v = run([sys.executable, "validate.py"], repo)
    attempts.append({"strategy": "EU/UK/CH → T+1 / tag 63 = 2 with effective_from; archive pre-T+1 mapping and golden sample",
                     "changes": ["mapping/settlement-cycle.yaml", "mapping/archive/settlement-cycle-pre-t1.yaml", "samples/fix/AE-xlon-regular-way.fix",
                                 "samples/fix/archive/AE-xlon-regular-way-pre-t1.fix", "README.md"],
                     "tests_ok": ok and v.ok, "test_summary": summary + " · validate.py: " + v.stdout.strip().replace("\n", "; ")})
    return attempts, branch


TRANCHE_1 = [("core-dates", migrate_core_dates), ("settlement-instruction-service", migrate_settlement_instruction_service),
             ("iso20022-fix-messages", migrate_iso20022)]


def pr_body(repo: str, attempts: list[dict], branch: str, owners: list[str]) -> str:
    final = attempts[-1]
    status = ("migrated" if all(a["tests_ok"] for a in attempts) else "migrated-with-deviation") if final["tests_ok"] else "blocked"
    lines = [f"# {PROGRAMME}: T+1 tranche 1 — {repo}", "",
             f"**Programme:** EU/UK/CH T+1 settlement, go-live {GO_LIVE} · **Branch:** `{branch}` · **Status:** `{status}`",
             target_line(repo), "",
             "Date-gated, not flipped: the cycle is resolved from the trade date, so pre-go-live trades and back-dated corrections keep "
             "settling T+2 and the same build is valid before and after go-live.", "",
             "## What changed", *[f"- `{c}`" for c in final["changes"]], "", "## Steps"]
    for i, a in enumerate(attempts, 1):
        lines.append(f"{i}. {a['strategy']} → {'PASS' if a['tests_ok'] else 'FAIL'} — `{a['test_summary']}`")
        if a.get("reason"):
            lines.append(f"   - note: {a['reason']}")
    lines += ["", "## Verification", f"- Repo tests: `{final['test_summary']}`",
              "- Golden-file parity: see `demo/reports/demo2-parity.html` (30 trades, old vs new, every difference classified).",
              "- Wire format: tag 64 stays `yyyyMMdd`; tag 63 becomes `2` only for EU/UK/CH trades dated on/after go-live.", "",
              "## Not in this PR (human decisions, see migration plan §Tranche 3)",
              "- Venue vs TARGET2 calendar divergence on 2027-12-23/24/30 trades — market-ops + Head of Post-Trade.",
              "- Ex-date regime for events straddling go-live (CA-2291) — asset-servicing ops.", "",
              f"CODEOWNERS reviewers: {', '.join(owners) or 'see repo CODEOWNERS'}", "", "Devin-Org: engineering"]
    return "\n".join(lines) + "\n"


def migrate_repo(name: str, fn) -> dict:
    repo = WORKSPACE / name
    started = now_iso()
    attempts, branch = fn(repo)
    git(repo, "add", "-A")
    git(repo, "commit", "-q", "-m", f"feature: {PROGRAMME} T+1 tranche 1 — {name}")
    patch = git(repo, "format-patch", "-1", "--stdout").stdout
    changed = [l.split("\t")[-1] for l in git(repo, "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").stdout.splitlines()]
    owners = sorted({o for p in changed for o in owners_for(repo, p)})
    pr_dir = OUT / "t1/prs" / name
    pr_dir.mkdir(parents=True, exist_ok=True)
    (pr_dir / "change.patch").write_text(patch)
    (pr_dir / "PR.md").write_text(pr_body(name, attempts, branch, owners))
    if any(not a["tests_ok"] for a in attempts[:-1]):
        for b in [l.strip("* ") for l in git(repo, "branch", "--list", f"{branch}-from-main").stdout.splitlines()]:
            (pr_dir / "attempt-1-from-main.patch").write_text(git(repo, "format-patch", "-1", "--stdout", b).stdout)
    status = ("migrated" if len(attempts) == 2 or all(a["tests_ok"] for a in attempts) else "migrated-with-deviation") if attempts[-1]["tests_ok"] else "blocked"
    log(f"{name:32s} {status:24s} {attempts[-1]['test_summary'][:110]}")
    return {"repo": name, "branch": branch, "status": status, "attempts": attempts, "owners": owners, "files": changed,
            "started_at": started, "finished_at": now_iso(), "pr_dir": str(pr_dir)}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--tranche", type=int, default=1, choices=[1])
    a = ap.parse_args()
    results = [migrate_repo(n, fn) for n, fn in TRANCHE_1]
    write_json(OUT / "t1/migration-results.json", {"tranche": a.tranche, "go_live": GO_LIVE, "results": results})
    for r in results:  # leave the workspace on the migrated branches so the parity harness and a reviewer see the new code
        git(WORKSPACE / r["repo"], "checkout", "-q", r["branch"])
    log(f"tranche {a.tranche}: " + ", ".join(f"{r['repo']}={r['status']}" for r in results))


if __name__ == "__main__":
    main()
