from risk_lib.limits import load_limits


def test_loads_limits():
    limits = load_limits("config/limits.yaml")
    assert limits["BARC"] == 250_000_000.0
