from datetime import date

from recon.expected import classify, expected_settlement


def test_unknown_venue_defaults_to_t_plus_two():
    assert expected_settlement(date(2027, 10, 11), "XLON") == date(2027, 10, 13)


def test_late_match_tolerance():
    assert classify(date(2027, 10, 13), date(2027, 10, 14)) == "LATE_MATCH"
    assert classify(date(2027, 10, 13), date(2027, 10, 15)) == "BREAK"
