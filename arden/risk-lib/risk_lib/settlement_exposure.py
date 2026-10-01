"""Settlement exposure: how long principal is at risk between trade and settlement."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

# Business-day settlement lag by market region. US/CA moved to 1 in May 2024.
SETTLEMENT_LAG_DAYS = {
    "US": 1,
    "CA": 1,
    "EU": 2,
    "UK": 2,
    "CH": 2,
    "JP": 2,
}


@dataclass(frozen=True)
class Exposure:
    trade_id: str
    region: str
    notional: float
    trade_date: date
    settlement_date: date
    days_at_risk: int
    exposure: float


def settlement_date(trade_date: date, region: str) -> date:
    lag = SETTLEMENT_LAG_DAYS[region]
    d = trade_date
    while lag > 0:
        d += timedelta(days=1)
        if d.weekday() < 5:
            lag -= 1
    return d


def settlement_exposure(trade_id: str, region: str, notional: float, trade_date: date,
                        daily_vol: float = 0.012) -> Exposure:
    sd = settlement_date(trade_date, region)
    days = (sd - trade_date).days
    # Square-root-of-time scaling of one-day P&L volatility over the calendar days at risk.
    exposure = notional * daily_vol * (days ** 0.5)
    return Exposure(trade_id, region, notional, trade_date, sd, days, exposure)
