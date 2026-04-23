#!/usr/bin/env python3
"""
Trade-to-Cash Movement Reconciliation Tool

Reconciles trades.csv against cash_movements.csv by:
  1. Matching cash movements to trades via reference fields and description parsing
  2. Comparing expected_cash (from trades) to actual amount (from cash movements)
  3. Flagging mismatches in amount, currency, settlement date, and counterparty
  4. Surfacing edge cases: unmatched records, netted settlements, duplicate
     references, currency discrepancies, and partial matches
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class Trade:
    trade_id: str
    trade_date: str
    settlement_date: str
    security: str
    security_name: str
    asset_class: str
    quantity: float
    price: float
    direction: str
    counterparty: str
    gross_amount: float
    expected_cash: float
    currency: str
    status: str
    trader: str


@dataclass
class CashMovement:
    txn_id: str
    value_date: str
    amount: float
    currency: str
    counterparty_name: str
    reference: str
    description: str
    account: str


@dataclass
class MatchResult:
    trade: Trade
    cash_movement: CashMovement
    match_method: str  # "direct_reference", "description_parse", "amount_match"
    issues: list[str] = field(default_factory=list)


@dataclass
class ReconciliationReport:
    matched: list[MatchResult] = field(default_factory=list)
    matched_with_issues: list[MatchResult] = field(default_factory=list)
    unmatched_trades: list[Trade] = field(default_factory=list)
    unmatched_cash: list[CashMovement] = field(default_factory=list)
    netted_settlements: list[CashMovement] = field(default_factory=list)
    duplicate_references: dict[str, list[CashMovement]] = field(default_factory=dict)
    currency_mismatches: list[MatchResult] = field(default_factory=list)
    amount_mismatches: list[MatchResult] = field(default_factory=list)
    date_mismatches: list[MatchResult] = field(default_factory=list)
    counterparty_mismatches: list[MatchResult] = field(default_factory=list)
    multi_cash_per_trade: dict[str, list[CashMovement]] = field(default_factory=dict)
    edge_cases: list[dict] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Loaders
# ---------------------------------------------------------------------------

def load_trades(path: str) -> list[Trade]:
    trades: list[Trade] = []
    with open(path, newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        for row in reader:
            trades.append(Trade(
                trade_id=row["trade_id"].strip(),
                trade_date=row["trade_date"].strip(),
                settlement_date=row["settlement_date"].strip(),
                security=row["security"].strip(),
                security_name=row["security_name"].strip(),
                asset_class=row["asset_class"].strip(),
                quantity=float(row["quantity"]),
                price=float(row["price"]),
                direction=row["direction"].strip(),
                counterparty=row["counterparty"].strip(),
                gross_amount=float(row["gross_amount"]),
                expected_cash=float(row["expected_cash"]),
                currency=row["currency"].strip(),
                status=row["status"].strip(),
                trader=row["trader"].strip(),
            ))
    return trades


def load_cash_movements(path: str) -> list[CashMovement]:
    movements: list[CashMovement] = []
    with open(path, newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        for row in reader:
            movements.append(CashMovement(
                txn_id=row["txn_id"].strip(),
                value_date=row["value_date"].strip(),
                amount=float(row["amount"]),
                currency=row["currency"].strip(),
                counterparty_name=row["counterparty_name"].strip(),
                reference=row["reference"].strip(),
                description=row["description"].strip(),
                account=row["account"].strip(),
            ))
    return movements


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

TRADE_ID_RE = re.compile(r"TRD-\d{4}-\d{6}")

COUNTERPARTY_ALIASES: dict[str, str] = {
    "BOFA": "Bank of America",
    "BANK OF AMERICA NA": "Bank of America",
    "BofA Securities": "Bank of America",
    "JPMC": "JPMorgan Chase",
    "JP MORGAN CHASE": "JPMorgan Chase",
    "JPMORGAN CHASE BANK NA": "JPMorgan Chase",
    "JPM Securities": "JPMorgan Chase",
    "CITIBANK NA": "Citigroup",
    "Citi": "Citigroup",
    "CITIGROUP GLOBAL MKTS": "Citigroup",
    "Citigroup Inc": "Citigroup",
    "GS INTL": "Goldman Sachs",
    "GOLDMAN SACHS": "Goldman Sachs",
    "GOLDMAN SACHS & CO LLC": "Goldman Sachs",
    "Morgan Stanley": "Morgan Stanley",
    "MORGAN STANLEY INTL": "Morgan Stanley",
    "MS & CO LLC": "Morgan Stanley",
    "HSBC": "HSBC",
    "HSBC Securities": "HSBC",
    "HSBC HOLDINGS": "HSBC",
    "HSBC BANK PLC": "HSBC",
    "UBS": "UBS",
    "UBS AG": "UBS",
    "UBS Securities LLC": "UBS",
    "UBS GROUP AG": "UBS",
    "RBC": "RBC Capital",
    "RBC CM": "RBC Capital",
    "RBC CAPITAL MARKETS": "RBC Capital",
    "Royal Bank of Canada": "RBC Capital",
    "Nomura": "Nomura",
    "NOMURA HOLDINGS": "Nomura",
    "NOMURA INTL PLC": "Nomura",
    "NOMURA SECURITIES INTL": "Nomura",
    "Barclays": "Barclays",
    "BARCLAYS INTL": "Barclays",
    "BARCLAYS CAPITAL INC": "Barclays",
    "BARCLAYS PLC": "Barclays",
    "Jefferies": "Jefferies",
    "JEFFERIES LLC": "Jefferies",
    "JEFFERIES GROUP": "Jefferies",
    "JEFFERIES & CO": "Jefferies",
    "BNP Paribas": "BNP Paribas",
    "BNP PARIBAS": "BNP Paribas",
    "BNP PARIBAS SA": "BNP Paribas",
    "BNP Securities": "BNP Paribas",
    "BNPP": "BNP Paribas",
    "Credit Suisse": "Credit Suisse",
    "CREDIT SUISSE AG": "Credit Suisse",
    "CREDIT SUISSE SECS": "Credit Suisse",
    "CS Securities": "Credit Suisse",
    "Deutsche Bank": "Deutsche Bank",
    "DEUTSCHE BANK AG": "Deutsche Bank",
    "DEUTSCHE BK": "Deutsche Bank",
    "DB Securities": "Deutsche Bank",
    "Wells Fargo": "Wells Fargo",
    "Wells Fargo Bank": "Wells Fargo",
    "WELLS FARGO & CO": "Wells Fargo",
    "WELLS FARGO SECS LLC": "Wells Fargo",
    "WFC": "Wells Fargo",
}


def normalize_counterparty(name: str) -> str:
    """Map variant counterparty names to a canonical form."""
    stripped = name.strip()
    if stripped in COUNTERPARTY_ALIASES:
        return COUNTERPARTY_ALIASES[stripped]
    return stripped


def parse_date_mm_dd_yyyy(date_str: str) -> Optional[datetime]:
    """Parse MM/DD/YYYY format from cash movements."""
    try:
        return datetime.strptime(date_str, "%m/%d/%Y")
    except ValueError:
        return None


def parse_date_iso(date_str: str) -> Optional[datetime]:
    """Parse YYYY-MM-DD format from trades."""
    try:
        return datetime.strptime(date_str, "%Y-%m-%d")
    except ValueError:
        return None


def extract_trade_ids_from_text(text: str) -> list[str]:
    """Extract all TRD-YYYY-XXXXXX patterns from a string."""
    return TRADE_ID_RE.findall(text)


def amounts_match(expected: float, actual: float, tolerance: float = 0.01) -> bool:
    """Check if two amounts match within tolerance."""
    return abs(expected - actual) <= tolerance


# ---------------------------------------------------------------------------
# Reconciliation engine
# ---------------------------------------------------------------------------

def reconcile(trades: list[Trade], cash_movements: list[CashMovement]) -> ReconciliationReport:
    report = ReconciliationReport()

    # Index trades by ID
    trade_by_id: dict[str, Trade] = {t.trade_id: t for t in trades}

    # Track which records have been matched
    matched_trade_ids: set[str] = set()
    matched_cm_txn_ids: set[str] = set()

    # Build indexes for cash movements
    cm_by_direct_ref: dict[str, list[CashMovement]] = defaultdict(list)
    cm_by_desc_ref: dict[str, list[CashMovement]] = defaultdict(list)

    for cm in cash_movements:
        # Check for netted settlements early
        if cm.reference.startswith("NET-"):
            report.netted_settlements.append(cm)
            matched_cm_txn_ids.add(cm.txn_id)
            continue

        # Direct reference match
        if cm.reference.startswith("TRD-"):
            cm_by_direct_ref[cm.reference].append(cm)

        # Description-based match
        desc_trade_ids = extract_trade_ids_from_text(cm.description)
        for tid in desc_trade_ids:
            cm_by_desc_ref[tid].append(cm)

        # Also check if REF-based reference has a trade ID embedded in description
        ref_trade_ids = extract_trade_ids_from_text(cm.reference)
        for tid in ref_trade_ids:
            if tid not in cm_by_direct_ref or cm not in cm_by_direct_ref[tid]:
                cm_by_direct_ref[tid].append(cm)

    # Detect duplicate references (multiple cash movements pointing to same trade)
    for tid, cms in cm_by_direct_ref.items():
        if len(cms) > 1:
            report.duplicate_references[tid] = cms

    # Also check description-based refs for duplicates
    for tid, cms in cm_by_desc_ref.items():
        if tid not in cm_by_direct_ref and len(cms) > 1:
            report.duplicate_references[tid] = cms

    # --- Pass 1: Direct reference matching ---
    for tid, cms in cm_by_direct_ref.items():
        if tid not in trade_by_id:
            continue
        trade = trade_by_id[tid]
        for cm in cms:
            if cm.txn_id in matched_cm_txn_ids:
                continue
            match = _validate_match(trade, cm, "direct_reference")
            _classify_match(match, report)
            matched_trade_ids.add(tid)
            matched_cm_txn_ids.add(cm.txn_id)

            # Track multi-cash per trade
            if tid not in report.multi_cash_per_trade:
                report.multi_cash_per_trade[tid] = [cm]
            else:
                report.multi_cash_per_trade[tid].append(cm)

    # --- Pass 2: Description-based matching ---
    for tid, cms in cm_by_desc_ref.items():
        if tid not in trade_by_id:
            continue
        trade = trade_by_id[tid]
        for cm in cms:
            if cm.txn_id in matched_cm_txn_ids:
                continue
            match = _validate_match(trade, cm, "description_parse")
            _classify_match(match, report)
            matched_trade_ids.add(tid)
            matched_cm_txn_ids.add(cm.txn_id)

            if tid not in report.multi_cash_per_trade:
                report.multi_cash_per_trade[tid] = [cm]
            else:
                report.multi_cash_per_trade[tid].append(cm)

    # --- Pass 3: Fuzzy amount + counterparty matching for remaining ---
    unmatched_trades_pass3 = [t for t in trades if t.trade_id not in matched_trade_ids]
    unmatched_cm_pass3 = [cm for cm in cash_movements
                          if cm.txn_id not in matched_cm_txn_ids]

    # Build amount index for remaining cash movements
    cm_by_amount: dict[float, list[CashMovement]] = defaultdict(list)
    for cm in unmatched_cm_pass3:
        cm_by_amount[round(cm.amount, 2)].append(cm)

    for trade in unmatched_trades_pass3:
        expected = round(trade.expected_cash, 2)
        candidates = cm_by_amount.get(expected, [])
        matched_cm = None
        for cm in candidates:
            if cm.txn_id in matched_cm_txn_ids:
                continue
            norm_cm = normalize_counterparty(cm.counterparty_name)
            norm_trade = normalize_counterparty(trade.counterparty)
            if norm_cm == norm_trade:
                matched_cm = cm
                break

        if matched_cm:
            match = _validate_match(trade, matched_cm, "amount_counterparty_match")
            _classify_match(match, report)
            matched_trade_ids.add(trade.trade_id)
            matched_cm_txn_ids.add(matched_cm.txn_id)

    # --- Clean up multi_cash_per_trade to only keep trades with >1 movement ---
    report.multi_cash_per_trade = {
        tid: cms for tid, cms in report.multi_cash_per_trade.items()
        if len(cms) > 1
    }

    # --- Collect unmatched ---
    report.unmatched_trades = [t for t in trades if t.trade_id not in matched_trade_ids]
    report.unmatched_cash = [cm for cm in cash_movements
                             if cm.txn_id not in matched_cm_txn_ids]

    # --- Detect edge cases ---
    _detect_edge_cases(report, trades, cash_movements)

    return report


def _validate_match(trade: Trade, cm: CashMovement, method: str) -> MatchResult:
    """Validate a trade-to-cash-movement match and collect issues."""
    issues: list[str] = []

    # Amount check
    if not amounts_match(trade.expected_cash, cm.amount):
        diff = cm.amount - trade.expected_cash
        issues.append(
            f"AMOUNT MISMATCH: expected {trade.expected_cash:,.2f}, "
            f"got {cm.amount:,.2f} (diff: {diff:+,.2f})"
        )

    # Currency check
    if trade.currency != cm.currency:
        issues.append(
            f"CURRENCY MISMATCH: trade={trade.currency}, cash={cm.currency}"
        )

    # Settlement date vs value date check
    settle_dt = parse_date_iso(trade.settlement_date)
    value_dt = parse_date_mm_dd_yyyy(cm.value_date)
    if settle_dt and value_dt and settle_dt.date() != value_dt.date():
        issues.append(
            f"DATE MISMATCH: settlement={trade.settlement_date}, "
            f"value_date={cm.value_date}"
        )

    # Counterparty check
    norm_trade_cp = normalize_counterparty(trade.counterparty)
    norm_cm_cp = normalize_counterparty(cm.counterparty_name)
    if norm_trade_cp != norm_cm_cp:
        issues.append(
            f"COUNTERPARTY MISMATCH: trade='{trade.counterparty}' "
            f"(norm: '{norm_trade_cp}'), "
            f"cash='{cm.counterparty_name}' (norm: '{norm_cm_cp}')"
        )

    return MatchResult(
        trade=trade,
        cash_movement=cm,
        match_method=method,
        issues=issues,
    )


def _classify_match(match: MatchResult, report: ReconciliationReport) -> None:
    """Put the match in the right bucket of the report."""
    if match.issues:
        report.matched_with_issues.append(match)
        for issue in match.issues:
            if "AMOUNT MISMATCH" in issue:
                report.amount_mismatches.append(match)
            if "CURRENCY MISMATCH" in issue:
                report.currency_mismatches.append(match)
            if "DATE MISMATCH" in issue:
                report.date_mismatches.append(match)
            if "COUNTERPARTY MISMATCH" in issue:
                report.counterparty_mismatches.append(match)
    else:
        report.matched.append(match)


def _detect_edge_cases(
    report: ReconciliationReport,
    trades: list[Trade],
    cash_movements: list[CashMovement],
) -> None:
    """Detect and catalog various edge cases."""

    # 1. Trades with CANCELLED or non-CONFIRMED status
    for t in trades:
        if t.status != "CONFIRMED":
            report.edge_cases.append({
                "type": "NON_CONFIRMED_TRADE",
                "trade_id": t.trade_id,
                "status": t.status,
                "detail": f"Trade {t.trade_id} has status '{t.status}' "
                          f"({t.security} {t.direction} {t.quantity})",
            })

    # 2. Cash movements with zero amounts
    for cm in cash_movements:
        if cm.amount == 0:
            report.edge_cases.append({
                "type": "ZERO_AMOUNT_CASH",
                "txn_id": cm.txn_id,
                "detail": f"Cash movement {cm.txn_id} has zero amount",
            })

    # 3. Very large single transactions (> $15M)
    for cm in cash_movements:
        if abs(cm.amount) > 15_000_000:
            report.edge_cases.append({
                "type": "LARGE_TRANSACTION",
                "txn_id": cm.txn_id,
                "amount": cm.amount,
                "detail": f"{cm.txn_id}: {cm.amount:,.2f} {cm.currency} "
                          f"({cm.counterparty_name})",
            })

    # 4. Cash movements referencing non-existent trade IDs
    trade_ids = {t.trade_id for t in trades}
    for cm in cash_movements:
        refs_in_cm = extract_trade_ids_from_text(cm.reference)
        refs_in_desc = extract_trade_ids_from_text(cm.description)
        for tid in set(refs_in_cm + refs_in_desc):
            if tid not in trade_ids:
                report.edge_cases.append({
                    "type": "ORPHAN_TRADE_REFERENCE",
                    "txn_id": cm.txn_id,
                    "referenced_trade": tid,
                    "detail": f"Cash {cm.txn_id} references {tid} "
                              f"which does not exist in trades file",
                })

    # 5. Same-day opposing trades (potential wash trades)
    trades_by_date_sec: dict[tuple[str, str], list[Trade]] = defaultdict(list)
    for t in trades:
        trades_by_date_sec[(t.trade_date, t.security)].append(t)
    for key, group in trades_by_date_sec.items():
        buys = [t for t in group if t.direction == "BUY"]
        sells = [t for t in group if t.direction == "SELL"]
        if buys and sells:
            report.edge_cases.append({
                "type": "SAME_DAY_OPPOSING",
                "date": key[0],
                "security": key[1],
                "buy_count": len(buys),
                "sell_count": len(sells),
                "detail": f"{key[1]} on {key[0]}: "
                          f"{len(buys)} BUY(s) and {len(sells)} SELL(s)",
            })

    # 6. Weekend/holiday settlement dates (Saturday=5, Sunday=6)
    for t in trades:
        dt = parse_date_iso(t.settlement_date)
        if dt and dt.weekday() >= 5:
            report.edge_cases.append({
                "type": "WEEKEND_SETTLEMENT",
                "trade_id": t.trade_id,
                "settlement_date": t.settlement_date,
                "day": dt.strftime("%A"),
                "detail": f"{t.trade_id} settles on {dt.strftime('%A')} "
                          f"({t.settlement_date})",
            })

    # 7. Cash movements whose sign disagrees with trade direction
    for m in report.matched + report.matched_with_issues:
        t = m.trade
        cm = m.cash_movement
        if t.direction == "BUY" and cm.amount > 0:
            report.edge_cases.append({
                "type": "SIGN_DIRECTION_MISMATCH",
                "trade_id": t.trade_id,
                "txn_id": cm.txn_id,
                "detail": f"BUY trade {t.trade_id} has positive cash "
                          f"movement {cm.amount:,.2f} (expected negative)",
            })
        elif t.direction == "SELL" and cm.amount < 0:
            report.edge_cases.append({
                "type": "SIGN_DIRECTION_MISMATCH",
                "trade_id": t.trade_id,
                "txn_id": cm.txn_id,
                "detail": f"SELL trade {t.trade_id} has negative cash "
                          f"movement {cm.amount:,.2f} (expected positive)",
            })


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def format_report(report: ReconciliationReport, trades: list[Trade],
                  cash_movements: list[CashMovement]) -> str:
    """Generate a human-readable markdown report."""
    lines: list[str] = []
    sep = "-" * 80

    lines.append("# Trade Reconciliation Report")
    lines.append("")
    lines.append("## Executive Summary")
    lines.append("")
    total_trades = len(trades)
    total_cash = len(cash_movements)
    clean = len(report.matched)
    issues = len(report.matched_with_issues)
    unmatched_t = len(report.unmatched_trades)
    unmatched_c = len(report.unmatched_cash)
    netted = len(report.netted_settlements)

    lines.append("| Metric | Count |")
    lines.append("|---|---|")
    lines.append(f"| Total trades | {total_trades} |")
    lines.append(f"| Total cash movements | {total_cash} |")
    lines.append(f"| Clean matches (no issues) | {clean} |")
    lines.append(f"| Matched with issues | {issues} |")
    lines.append(f"| Unmatched trades | {unmatched_t} |")
    lines.append(f"| Unmatched cash movements | {unmatched_c} |")
    lines.append(f"| Netted settlements | {netted} |")
    lines.append(f"| Amount mismatches | {len(report.amount_mismatches)} |")
    lines.append(f"| Currency mismatches | {len(report.currency_mismatches)} |")
    lines.append(f"| Date mismatches | {len(report.date_mismatches)} |")
    lines.append(f"| Counterparty mismatches | {len(report.counterparty_mismatches)} |")
    lines.append(f"| Trades with multiple cash movements | {len(report.multi_cash_per_trade)} |")
    lines.append(f"| Edge cases detected | {len(report.edge_cases)} |")
    lines.append("")

    match_rate = (clean + issues) / total_trades * 100 if total_trades else 0
    lines.append(f"**Overall match rate: {match_rate:.1f}%** "
                 f"({clean + issues} of {total_trades} trades matched)")
    lines.append("")

    # --- Amount mismatches ---
    if report.amount_mismatches:
        lines.append(sep)
        lines.append("## Amount Mismatches")
        lines.append("")
        lines.append("| Trade ID | Expected Cash | Actual Cash | Difference | "
                     "Security | Direction |")
        lines.append("|---|---|---|---|---|---|")
        for m in sorted(report.amount_mismatches,
                        key=lambda x: abs(x.cash_movement.amount - x.trade.expected_cash),
                        reverse=True):
            diff = m.cash_movement.amount - m.trade.expected_cash
            lines.append(
                f"| {m.trade.trade_id} | "
                f"{m.trade.expected_cash:,.2f} | "
                f"{m.cash_movement.amount:,.2f} | "
                f"{diff:+,.2f} | "
                f"{m.trade.security} | "
                f"{m.trade.direction} |"
            )
        lines.append("")

    # --- Currency mismatches ---
    if report.currency_mismatches:
        lines.append(sep)
        lines.append("## Currency Mismatches")
        lines.append("")
        lines.append("| Trade ID | Trade CCY | Cash CCY | Amount | "
                     "Security | Description |")
        lines.append("|---|---|---|---|---|---|")
        for m in report.currency_mismatches:
            lines.append(
                f"| {m.trade.trade_id} | "
                f"{m.trade.currency} | "
                f"{m.cash_movement.currency} | "
                f"{m.cash_movement.amount:,.2f} | "
                f"{m.trade.security} | "
                f"{m.cash_movement.description} |"
            )
        lines.append("")

    # --- Date mismatches ---
    if report.date_mismatches:
        lines.append(sep)
        lines.append("## Settlement Date vs Value Date Mismatches")
        lines.append("")
        lines.append("| Trade ID | Settlement Date | Value Date | "
                     "Security | Direction |")
        lines.append("|---|---|---|---|---|")
        for m in report.date_mismatches:
            lines.append(
                f"| {m.trade.trade_id} | "
                f"{m.trade.settlement_date} | "
                f"{m.cash_movement.value_date} | "
                f"{m.trade.security} | "
                f"{m.trade.direction} |"
            )
        lines.append("")

    # --- Counterparty mismatches ---
    if report.counterparty_mismatches:
        lines.append(sep)
        lines.append("## Counterparty Mismatches")
        lines.append("")
        lines.append("After normalizing common aliases, these still don't match:")
        lines.append("")
        lines.append("| Trade ID | Trade Counterparty | Cash Counterparty | "
                     "Amount |")
        lines.append("|---|---|---|---|")
        for m in report.counterparty_mismatches:
            lines.append(
                f"| {m.trade.trade_id} | "
                f"{m.trade.counterparty} | "
                f"{m.cash_movement.counterparty_name} | "
                f"{m.cash_movement.amount:,.2f} |"
            )
        lines.append("")

    # --- Netted settlements ---
    if report.netted_settlements:
        lines.append(sep)
        lines.append("## Netted Settlements")
        lines.append("")
        lines.append("These cash movements represent netted settlements of "
                     "multiple trades and cannot be matched 1:1:")
        lines.append("")
        lines.append("| Txn ID | Value Date | Amount | Counterparty | "
                     "Reference | Description |")
        lines.append("|---|---|---|---|---|---|")
        for cm in report.netted_settlements:
            lines.append(
                f"| {cm.txn_id} | {cm.value_date} | "
                f"{cm.amount:,.2f} | {cm.counterparty_name} | "
                f"{cm.reference} | {cm.description} |"
            )
        lines.append("")

    # --- Trades with multiple cash movements ---
    if report.multi_cash_per_trade:
        lines.append(sep)
        lines.append("## Trades with Multiple Cash Movements")
        lines.append("")
        for tid, cms in report.multi_cash_per_trade.items():
            lines.append(f"### {tid}")
            lines.append("")
            lines.append("| Txn ID | Amount | Value Date | Description |")
            lines.append("|---|---|---|---|")
            for cm in cms:
                lines.append(
                    f"| {cm.txn_id} | {cm.amount:,.2f} | "
                    f"{cm.value_date} | {cm.description} |"
                )
            lines.append("")

    # --- Unmatched trades ---
    if report.unmatched_trades:
        lines.append(sep)
        lines.append("## Unmatched Trades")
        lines.append("")
        lines.append(f"{len(report.unmatched_trades)} trades have no "
                     "corresponding cash movement:")
        lines.append("")
        lines.append("| Trade ID | Settlement | Security | Direction | "
                     "Expected Cash | Counterparty | Trader |")
        lines.append("|---|---|---|---|---|---|---|")
        for t in sorted(report.unmatched_trades,
                        key=lambda x: abs(x.expected_cash), reverse=True):
            lines.append(
                f"| {t.trade_id} | {t.settlement_date} | "
                f"{t.security} | {t.direction} | "
                f"{t.expected_cash:,.2f} | "
                f"{t.counterparty} | {t.trader} |"
            )
        lines.append("")

    # --- Unmatched cash movements ---
    if report.unmatched_cash:
        lines.append(sep)
        lines.append("## Unmatched Cash Movements")
        lines.append("")
        lines.append(f"{len(report.unmatched_cash)} cash movements have no "
                     "corresponding trade:")
        lines.append("")
        lines.append("| Txn ID | Value Date | Amount | Currency | "
                     "Counterparty | Reference | Description |")
        lines.append("|---|---|---|---|---|---|---|")
        for cm in sorted(report.unmatched_cash,
                         key=lambda x: abs(x.amount), reverse=True):
            lines.append(
                f"| {cm.txn_id} | {cm.value_date} | "
                f"{cm.amount:,.2f} | {cm.currency} | "
                f"{cm.counterparty_name} | {cm.reference} | "
                f"{cm.description} |"
            )
        lines.append("")

    # --- Edge cases ---
    if report.edge_cases:
        lines.append(sep)
        lines.append("## Edge Cases")
        lines.append("")

        # Group by type
        by_type: dict[str, list[dict]] = defaultdict(list)
        for ec in report.edge_cases:
            by_type[ec["type"]].append(ec)

        for ec_type, cases in sorted(by_type.items()):
            lines.append(f"### {ec_type.replace('_', ' ').title()} "
                         f"({len(cases)} occurrences)")
            lines.append("")
            for c in cases[:20]:  # cap at 20 per type for readability
                lines.append(f"- {c['detail']}")
            if len(cases) > 20:
                lines.append(f"- ... and {len(cases) - 20} more")
            lines.append("")

    return "\n".join(lines)


def write_json_report(report: ReconciliationReport, path: str) -> None:
    """Write machine-readable JSON report for downstream processing."""
    data = {
        "summary": {
            "clean_matches": len(report.matched),
            "matches_with_issues": len(report.matched_with_issues),
            "unmatched_trades": len(report.unmatched_trades),
            "unmatched_cash_movements": len(report.unmatched_cash),
            "netted_settlements": len(report.netted_settlements),
            "amount_mismatches": len(report.amount_mismatches),
            "currency_mismatches": len(report.currency_mismatches),
            "date_mismatches": len(report.date_mismatches),
            "counterparty_mismatches": len(report.counterparty_mismatches),
            "edge_cases": len(report.edge_cases),
        },
        "amount_mismatches": [
            {
                "trade_id": m.trade.trade_id,
                "expected": m.trade.expected_cash,
                "actual": m.cash_movement.amount,
                "diff": round(m.cash_movement.amount - m.trade.expected_cash, 2),
                "security": m.trade.security,
            }
            for m in report.amount_mismatches
        ],
        "currency_mismatches": [
            {
                "trade_id": m.trade.trade_id,
                "trade_currency": m.trade.currency,
                "cash_currency": m.cash_movement.currency,
                "txn_id": m.cash_movement.txn_id,
            }
            for m in report.currency_mismatches
        ],
        "unmatched_trades": [
            {
                "trade_id": t.trade_id,
                "expected_cash": t.expected_cash,
                "security": t.security,
                "counterparty": t.counterparty,
                "settlement_date": t.settlement_date,
            }
            for t in report.unmatched_trades
        ],
        "unmatched_cash_movements": [
            {
                "txn_id": cm.txn_id,
                "amount": cm.amount,
                "reference": cm.reference,
                "counterparty": cm.counterparty_name,
            }
            for cm in report.unmatched_cash
        ],
        "netted_settlements": [
            {
                "txn_id": cm.txn_id,
                "amount": cm.amount,
                "reference": cm.reference,
                "description": cm.description,
            }
            for cm in report.netted_settlements
        ],
        "edge_cases": report.edge_cases,
    }

    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, default=str)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Reconcile trades against cash movements"
    )
    parser.add_argument(
        "--trades", required=True, help="Path to trades.csv"
    )
    parser.add_argument(
        "--cash", required=True, help="Path to cash_movements.csv"
    )
    parser.add_argument(
        "--output-dir", default=".", help="Directory for output reports"
    )
    parser.add_argument(
        "--json", action="store_true",
        help="Also output machine-readable JSON report"
    )
    args = parser.parse_args()

    # Load data
    print(f"Loading trades from {args.trades}...")
    trades = load_trades(args.trades)
    print(f"  -> {len(trades)} trades loaded")

    print(f"Loading cash movements from {args.cash}...")
    cash_movements = load_cash_movements(args.cash)
    print(f"  -> {len(cash_movements)} cash movements loaded")

    # Reconcile
    print("\nRunning reconciliation...")
    report = reconcile(trades, cash_movements)

    # Output
    os.makedirs(args.output_dir, exist_ok=True)

    md_path = os.path.join(args.output_dir, "reconciliation_report.md")
    md_content = format_report(report, trades, cash_movements)
    with open(md_path, "w", encoding="utf-8") as fh:
        fh.write(md_content)
    print(f"\nMarkdown report written to {md_path}")

    if args.json:
        json_path = os.path.join(args.output_dir, "reconciliation_report.json")
        write_json_report(report, json_path)
        print(f"JSON report written to {json_path}")

    # Print summary to stdout
    print("\n" + "=" * 60)
    print("RECONCILIATION SUMMARY")
    print("=" * 60)
    print(f"  Clean matches:            {len(report.matched)}")
    print(f"  Matched with issues:      {len(report.matched_with_issues)}")
    print(f"  Unmatched trades:         {len(report.unmatched_trades)}")
    print(f"  Unmatched cash movements: {len(report.unmatched_cash)}")
    print(f"  Netted settlements:       {len(report.netted_settlements)}")
    print(f"  Amount mismatches:        {len(report.amount_mismatches)}")
    print(f"  Currency mismatches:      {len(report.currency_mismatches)}")
    print(f"  Date mismatches:          {len(report.date_mismatches)}")
    print(f"  Counterparty mismatches:  {len(report.counterparty_mismatches)}")
    print(f"  Edge cases:               {len(report.edge_cases)}")
    print("=" * 60)


if __name__ == "__main__":
    main()
