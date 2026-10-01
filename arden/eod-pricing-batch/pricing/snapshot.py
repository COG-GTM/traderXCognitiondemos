"""Builds the EOD snapshot from an ArdenFeed quote batch (schema v2.2)."""
from __future__ import annotations

import json
from dataclasses import dataclass


@dataclass(frozen=True)
class Price:
    isin: str
    px: float
    ccy: str
    asof: str


def parse_quote(q: dict) -> Price:
    px = q["px"]
    # ArdenFeed v2.3 (25 Sep 2026) wraps px as {"v": number, "ccy": str}; v2.2 sends a bare number.
    if isinstance(px, dict):
        return Price(q["isin"], float(px["v"]), px.get("ccy", q.get("ccy", "USD")), q["asof"])
    return Price(q["isin"], float(px), q["ccy"], q["asof"])


def build_snapshot(batch_json: str) -> list[Price]:
    batch = json.loads(batch_json)
    return [parse_quote(q) for q in batch["quotes"]]
