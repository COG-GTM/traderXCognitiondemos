#!/usr/bin/env python3
"""Generate the synthetic TechFest fixtures from one position list.

Outputs (all under position-service/techfest-migration/fixtures/):
  seed-positions.sql        rows appended to database/initialSchema.sql (copy verbatim)
  v1-positions-77007.json   legacy GET /positions/77007 response
  v2-pages-77007.json       approved GET /v2/positions pages (default limit 10)
  report-expectations.json  what the Portfolio report must show for 77007
  v2-pages-77007-seeded-defect.json  presenter-only: page 2 drops one row

Run:  python3 position-service/techfest-migration/scripts/generate_fixtures.py
"""
import base64
import json
import os
from datetime import datetime, timezone
from decimal import Decimal

ACCOUNT = 77007
PAGE_SIZE = 10
SCALE = {"JPY": 0, "USD": 2, "EUR": 2, "GBP": 2, "CHF": 2}

# security, currency, quantity, market value (None = unknown), as-of minute offset
POSITIONS = [
    ("ABBN", "CHF", 1200, "58344.00", 0),
    ("ADS", "EUR", 300, "67560.00", 1),
    ("AIR", "EUR", 450, "70155.00", 2),
    ("AZN", "GBP", 800, "97840.00", 3),
    ("BARC", "GBP", 15000, "31350.00", 4),
    ("BMW", "EUR", 700, "62965.00", 5),
    ("BP", "GBP", 9000, "42120.00", 6),
    ("C", "USD", -2000, "-128400.00", 7),
    ("DBK", "EUR", 4000, "61200.00", 8),
    ("HSBA", "GBP", 6000, "40260.00", 9),
    ("IBM", "USD", -100, "-19850.00", 10),
    ("MC", "EUR", 150, None, 11),
    ("MS", "USD", 1000, "98750.00", 12),
    ("NESN", "CHF", 900, "78300.00", 13),
    ("NOVN", "CHF", 1100, "104500.00", 14),
    ("OR", "EUR", 250, "101250.00", 15),
    ("RIO", "GBP", 1500, "78450.00", 16),
    ("ROG", "CHF", 400, "112400.00", 17),
    ("SAP", "EUR", 600, "115800.00", 18),
    ("SHEL", "GBP", 3200, "85760.00", 19),
    ("SIE", "EUR", 500, "88250.00", 20),
    ("T6758", "JPY", 2000, "27400000", 21),
    ("T7203", "JPY", 3000, "8460000", 22),
    ("T9984", "JPY", 1000, None, 23),
    ("UBSG", "CHF", 2500, "67750.00", 24),
    ("VOD", "GBP", 40000, "29800.00", 25),
]

BASE = datetime(2026, 9, 22, 16, 0, 0, tzinfo=timezone.utc)


def as_of(offset):
    return BASE.replace(minute=offset)


def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def amount(value, ccy):
    q = Decimal(1).scaleb(-SCALE[ccy])
    return str(Decimal(value).quantize(q))


def cursor(security):
    return base64.urlsafe_b64encode(security.encode()).decode().rstrip("=")


def main():
    out = os.path.join(os.path.dirname(__file__), "..", "fixtures")
    os.makedirs(out, exist_ok=True)
    rows = sorted(POSITIONS, key=lambda p: p[0])
    assert [p[0] for p in POSITIONS] == [p[0] for p in rows], "keep POSITIONS sorted by security"

    sql = ["-- TechFest synthetic multi-currency fixture account (Synthetic demonstration)",
           f"INSERT into Accounts (ID, DisplayName) VALUES ({ACCOUNT}, 'Synthetic Multi-Currency Book');",
           f"INSERT into AccountUsers (AccountID, Username) VALUES ({ACCOUNT}, 'user01');"]
    v1, items, expected_rows = [], [], []
    for sec, ccy, qty, mv, off in rows:
        ts = as_of(off)
        mv_sql = "NULL" if mv is None else mv
        sql.append(
            "INSERT into Positions (AccountID, Security, Updated, Quantity, Currency, MarketValue) "
            f"VALUES({ACCOUNT}, '{sec}', TIMESTAMP '{ts.strftime('%Y-%m-%d %H:%M:%S')}', {qty}, '{ccy}', {mv_sql});"
        )
        # legacy shape: Jackson Date -> "yyyy-MM-dd'T'HH:mm:ss.SSS+00:00", BigDecimal(19,4) -> JSON number
        v1.append({"accountId": ACCOUNT, "security": sec, "quantity": qty,
                   "updated": ts.strftime("%Y-%m-%dT%H:%M:%S.000+00:00"), "currency": ccy,
                   "marketValue": None if mv is None else float(Decimal(mv))})
        items.append({"accountId": ACCOUNT, "security": sec, "quantity": qty, "asOf": iso(ts), "currency": ccy,
                      "marketValue": None if mv is None else {"amount": amount(mv, ccy), "currency": ccy}})
        expected_rows.append({"security": sec, "quantity": qty, "asOf": iso(ts), "currency": ccy,
                              "marketValue": None if mv is None else amount(mv, ccy)})

    pages = []
    for i in range(0, len(items), PAGE_SIZE):
        chunk = items[i:i + PAGE_SIZE]
        last = chunk[-1]["security"]
        has_more = i + PAGE_SIZE < len(items)
        pages.append({"request": {"accountId": ACCOUNT, "limit": PAGE_SIZE,
                                  "cursor": None if i == 0 else cursor(items[i - 1]["security"])},
                      "response": {"items": chunk, "nextCursor": cursor(last) if has_more else None}})

    summaries = {}
    for sec, ccy, qty, mv, _ in rows:
        s = summaries.setdefault(ccy, {"currency": ccy, "count": 0, "total": Decimal(0), "incomplete": False})
        s["count"] += 1
        if mv is None:
            s["incomplete"] = True
        else:
            s["total"] += Decimal(mv)
    summary_rows = [{"currency": c, "count": s["count"], "totalMarketValue": amount(s["total"], c),
                     "incomplete": s["incomplete"]} for c, s in sorted(summaries.items())]

    defect = json.loads(json.dumps(pages))
    dropped = defect[1]["response"]["items"].pop(3)

    def dump(name, obj):
        with open(os.path.join(out, name), "w") as f:
            json.dump(obj, f, indent=2)
            f.write("\n")

    dump("v1-positions-77007.json", v1)
    dump("v2-pages-77007.json", pages)
    dump("report-expectations.json", {
        "label": "Synthetic demonstration",
        "accountId": ACCOUNT,
        "pageSize": PAGE_SIZE,
        "expectedPageCount": len(pages),
        "unknownMarketValueSecurities": [r[0] for r in rows if r[3] is None],
        "rows": expected_rows,
        "currencySummaries": summary_rows,
    })
    dump("v2-pages-77007-seeded-defect.json", {
        "scenario": "seeded-defect",
        "description": f"Page 2 silently drops {dropped['security']}; a consumer that trusts page counts misses it.",
        "droppedSecurity": dropped["security"],
        "pages": defect,
    })
    with open(os.path.join(out, "seed-positions.sql"), "w") as f:
        f.write("\n".join(sql) + "\n")
    print(f"wrote fixtures for account {ACCOUNT}: {len(rows)} positions, {len(pages)} pages")


if __name__ == "__main__":
    main()
