# Trade Reconciliation Tool

Reconciles `trades.csv` against `cash_movements.csv` to identify mismatches and edge cases in trade settlement flows.

## What It Does

1. **Matches** cash movements to trades via:
   - Direct trade ID references (`TRD-2026-XXXXXX` in the `reference` field)
   - Trade IDs embedded in cash movement descriptions
   - Fuzzy matching on amount + normalized counterparty name

2. **Flags mismatches** in:
   - Amount (expected vs actual cash)
   - Currency (trade currency vs cash movement currency)
   - Settlement date vs value date
   - Counterparty name (after alias normalization)

3. **Detects edge cases**:
   - Netted settlements (multiple trades settled as one payment)
   - Orphan trade references (cash movements referencing non-existent trades)
   - Same-day opposing trades (potential wash trades)
   - Weekend settlement dates
   - Sign/direction mismatches (e.g., BUY with positive cash flow)
   - Large transactions (>$15M)
   - Non-confirmed trade statuses
   - Multiple cash movements for a single trade

## Usage

```bash
python3 reconcile.py \
  --trades path/to/trades.csv \
  --cash path/to/cash_movements.csv \
  --output-dir ./output \
  --json
```

### Arguments

| Flag | Required | Description |
|------|----------|-------------|
| `--trades` | Yes | Path to the trades CSV file |
| `--cash` | Yes | Path to the cash movements CSV file |
| `--output-dir` | No | Directory for output reports (default: current directory) |
| `--json` | No | Also generate a machine-readable JSON report |

### Output

- `reconciliation_report.md` — Human-readable markdown report with tables
- `reconciliation_report.json` — Machine-readable JSON (when `--json` is passed)

## Input File Formats

### trades.csv
```
trade_id,trade_date,settlement_date,security,security_name,asset_class,quantity,price,direction,counterparty,gross_amount,expected_cash,currency,status,trader
```

### cash_movements.csv
```
txn_id,value_date,amount,currency,counterparty_name,reference,description,account
```

## Requirements

- Python 3.10+ (uses `list[str]` type hints and `from __future__ import annotations`)
- No external dependencies — stdlib only
