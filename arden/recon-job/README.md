# recon-job

Nightly job comparing our expected settlement date per trade with the custodian's MT535/MT536 view.

- **Owner:** `@ardencm/ops-recon`
- **Run:** `python -m recon.run fixtures/custodian.csv`
- **Test:** `python -m pytest`

The job ignores holiday calendars on purpose: recon runs after core-dates has stamped the trade, so it only
needs to be "close enough" to classify breaks. That assumption is documented in OPS-RECON-9.
