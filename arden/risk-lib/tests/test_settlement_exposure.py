from datetime import date

from risk_lib.settlement_exposure import settlement_date, settlement_exposure


def test_eu_trade_settles_two_business_days_later():
    assert settlement_date(date(2027, 10, 11), "EU") == date(2027, 10, 13)


def test_us_trade_settles_next_business_day():
    assert settlement_date(date(2027, 10, 11), "US") == date(2027, 10, 12)


def test_exposure_scales_with_days_at_risk():
    eu = settlement_exposure("T1", "EU", 1_000_000, date(2027, 10, 11))
    us = settlement_exposure("T2", "US", 1_000_000, date(2027, 10, 11))
    assert eu.days_at_risk == 2 and us.days_at_risk == 1
    assert eu.exposure > us.exposure
