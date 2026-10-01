import csv
import sys
from datetime import date

from recon.expected import classify, expected_settlement


def main(path: str) -> int:
    breaks = 0
    with open(path) as fh:
        for row in csv.DictReader(fh):
            exp = expected_settlement(date.fromisoformat(row["trade_date"]), row["mic"])
            status = classify(exp, date.fromisoformat(row["custodian_settlement_date"]))
            breaks += status == "BREAK"
            print(f"{row['trade_id']},{exp},{row['custodian_settlement_date']},{status}")
    return 1 if breaks else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
