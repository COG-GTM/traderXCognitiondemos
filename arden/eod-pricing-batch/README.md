# eod-pricing-batch

EOD snapshot builder for the ArdenFeed vendor feed. Runs at 18:30 Europe/London from `batch-scheduler-config`
(`EOD_PRICING`). The batch was patched for ArdenFeed schema v2.3 on 25 Sep 2026 (MD-1187) — the client portal
consumes the same feed intraday and was **not** part of that change.

- **Owner:** `@ardencm/market-data`
- **Test:** `python -m pytest`
