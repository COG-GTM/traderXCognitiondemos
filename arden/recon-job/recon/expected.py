"""Expected settlement date per trade, used to flag custodian breaks."""
from __future__ import annotations

from datetime import date, timedelta

# Venue -> business-day lag. Anything missing is assumed regular-way T+2.
VENUE_LAG = {"XNYS": 1, "XNAS": 1, "XTSE": 1}
DEFAULT_LAG = 2

# Recon tolerance: a custodian date within +1 business day of expected is a "late match", not a break.
LATE_MATCH_TOLERANCE_DAYS = 1


def expected_settlement(trade_date: date, mic: str) -> date:
    lag = VENUE_LAG.get(mic, DEFAULT_LAG)
    d = trade_date
    while lag > 0:
        d += timedelta(days=1)
        if d.weekday() < 5:
            lag -= 1
    return d


def classify(expected: date, reported: date) -> str:
    if reported == expected:
        return "MATCH"
    if 0 < (reported - expected).days <= LATE_MATCH_TOLERANCE_DAYS:
        return "LATE_MATCH"
    return "BREAK"
