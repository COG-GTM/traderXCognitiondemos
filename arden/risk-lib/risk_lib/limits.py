"""Counterparty settlement limits loaded from YAML."""
from __future__ import annotations

import yaml


def load_limits(path: str) -> dict[str, float]:
    with open(path) as fh:
        data = yaml.load(fh, Loader=yaml.Loader)
    return {row["counterparty"]: float(row["limit"]) for row in data["limits"]}
