# ArdenFeed schema change notice — v2.3

**From:** ArdenFeed Client Services  **Received:** 11 Sep 2026 (market-data shared inbox)
**Effective:** 25 Sep 2026 06:00 UTC (no parallel run)

`px` becomes `{ "v": number, "ccy": string }`. Quote-level `ccy` is deprecated and will be removed in v2.4.
Consumers should update parsers before the effective date.

Internal tracking: MD-1187 (eod-pricing-batch updated 22 Sep). No ticket was raised for client-portal.
