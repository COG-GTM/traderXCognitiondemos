# risk-lib

Settlement and counterparty exposure calculations used by the intraday risk dashboard and the EOD limit checks.

- **Owner:** `@ardencm/risk-quant`
- **Test:** `python -m pytest`
- **Install:** `pip install -e .[test]` (index via `pip.conf` → internal Artifactory PyPI remote)

`SETTLEMENT_LAG_DAYS` drives the days-at-risk horizon. Regions, not venues: the library does not know about
individual MICs or holiday calendars (see RISK-2210 for the long-standing request to use `core-dates` calendars).
