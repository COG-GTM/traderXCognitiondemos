from __future__ import annotations

import html
import re

REQUIRED_FAILURES = (
    "CustodyGatewayContractTest.tag64IsLocalMktDate",
    "InstructionBuilderTest.fixMessageCarriesLocalMktDate",
    "InstructionBuilderTest.londonInstructionSettlesTPlusTwo",
)

CONTROL_ENTITY = re.compile(r"(?i)&(?:amp;)?#(?:0*1|x0*1);")
TEST_MARKER = re.compile(r"([A-Za-z_$][\w$]*Test\.[A-Za-z_$][\w$]*):\s*")


def readable_test_summary(summary: str) -> str:
    text = summary.replace("\x01", " | ")
    text = CONTROL_ENTITY.sub(" | ", text)
    text = html.unescape(text).replace("\x01", " | ")
    return CONTROL_ENTITY.sub(" | ", text)


def hard_case_evidence(failed: dict, passed: dict) -> dict:
    summary = readable_test_summary(failed.get("test_summary", ""))
    failure_text = summary.partition("FAILED:")[2]
    markers = list(TEST_MARKER.finditer(failure_text))
    details = {
        marker.group(1): failure_text[marker.end():markers[index + 1].start() if index + 1 < len(markers) else len(failure_text)].strip().rstrip(", ")
        for index, marker in enumerate(markers)
    }
    missing = [name for name in REQUIRED_FAILURES if name not in details]
    if missing:
        raise ValueError(f"Demo 1 hard case is missing expected failure test(s): {', '.join(missing)}")

    tag64 = re.search(r"tag 64 must match (\^\[0-9\]\{8\}\$) but was (\d{4}-\d{2}-\d{2})", details[REQUIRED_FAILURES[0]])
    message_date = re.search(r"64=(\d{4}-\d{2}-\d{2})", details[REQUIRED_FAILURES[1]])
    settlement = re.search(r"expected: <(\d{8})> but was: <(\d{4}-\d{2}-\d{2})>", details[REQUIRED_FAILURES[2]])
    if not tag64 or not message_date or not settlement:
        raise ValueError("Demo 1 hard case is missing parseable tag 64 expected/actual evidence")

    actual_iso = tag64.group(2)
    expected_date = settlement.group(1)
    if (message_date.group(1) != actual_iso
            or settlement.group(2) != actual_iso
            or expected_date != actual_iso.replace("-", "")):
        raise ValueError("Demo 1 hard case test evidence disagrees on the tag 64 date")

    failed_version = re.search(r"core-dates ([\d.]+)", failed.get("strategy", ""))
    pin_version = re.search(r"pin core-dates ([\d.]+)", passed.get("strategy", ""))
    snakeyaml_version = re.search(r"SnakeYAML ([\d.]+)", passed.get("strategy", ""))
    if not failed_version or not pin_version or not snakeyaml_version:
        raise ValueError("Demo 1 hard case is missing version evidence from its attempt strategies")

    counts = re.match(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)", passed.get("test_summary", ""))
    if not counts or int(counts.group(2)) or int(counts.group(3)):
        raise ValueError("Demo 1 hard case passing attempt is missing successful test-count evidence")
    test_file = next((path for path in passed.get("changes", []) if path.endswith("Tag64WireFormatPinTest.java")), None)
    if not test_file:
        raise ValueError("Demo 1 hard case passing attempt is missing Tag64WireFormatPinTest.java")

    return {
        "headline": (
            f"FIX tag 64 (SettlDate) must be LocalMktDate {expected_date}; "
            f"core-dates {failed_version.group(1)} emits ISO {actual_iso}. The custody-gateway contract breaks."
        ),
        "attempt2_headline": (
            f"Pin core-dates {pin_version.group(1)}: security fix "
            f"(SnakeYAML {snakeyaml_version.group(1)}), wire format unchanged"
        ),
        "tests": [
            {
                "name": REQUIRED_FAILURES[0],
                "expected": tag64.group(1),
                "actual": actual_iso,
            },
            {
                "name": REQUIRED_FAILURES[1],
                "expected": f"64={expected_date}",
                "actual": f"64={message_date.group(1)}",
            },
            {
                "name": REQUIRED_FAILURES[2],
                "expected": expected_date,
                "actual": settlement.group(2),
            },
        ],
        "full_summary": summary,
        "passed": f"{int(counts.group(1)) - int(counts.group(4))}/{counts.group(1)}",
        "test_file": test_file,
        "actual_iso": actual_iso,
    }
