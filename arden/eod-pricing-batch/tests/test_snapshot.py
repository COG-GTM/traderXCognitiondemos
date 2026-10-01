import json

from pricing.snapshot import build_snapshot


def test_parses_v22_and_v23_shapes():
    batch = {"schema": "2.3", "quotes": [
        {"isin": "GB0002634946", "px": 12.34, "ccy": "GBP", "asof": "2026-09-24T16:35:00Z"},
        {"isin": "CH0012032048", "px": {"v": 271.5, "ccy": "CHF"}, "asof": "2026-09-25T15:30:00Z"},
    ]}
    out = build_snapshot(json.dumps(batch))
    assert [p.px for p in out] == [12.34, 271.5]
    assert out[1].ccy == "CHF"
