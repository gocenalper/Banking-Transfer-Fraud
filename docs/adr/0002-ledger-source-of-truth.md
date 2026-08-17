# ADR 0002 — Ledger as source of truth; balance as projection; reservation model

**Date:** 2026-08-14 · **Status:** Accepted

## Context
After ADR-0001 the ledger (`ledger_db`) and the account balance (`account_db`) live in
different services and different databases. "Where does the truth about money live?"
had to be settled. Options considered: (A) balance is the truth and the ledger is an
audit log, (B) ledger is the truth and balance is a projection, (C) full event sourcing.

## Decision: B plus a reservation model
- **The historical truth of booked money movements is the ledger**: append-only
  double-entry records (debit −, credit +, each transfer sums to zero). Entries are
  never updated or deleted; corrections are compensating entries.
- **`account.balance` is a derived projection** kept for fast reads and
  insufficient-funds checks; if it is ever corrupted it can be rebuilt from the ledger.
- **The authority on the instantaneous available balance is account-service**
  (reservation model): starting a transfer atomically reduces the available balance by
  creating a hold in its own database; when the saga completes, the hold is either
  captured into a ledger entry or released. One source of truth **per question**:
  available → account, booked → ledger.
- **Reconciliation is mandatory**: a periodic process compares `account.balance`
  against the ledger sum and reports drift — the two databases are only eventually
  consistent, so this is not optional.

## Rejected
- **A:** a movement that fails to reach the ledger would be a lost audit trail —
  unacceptable in banking.
- **C:** event versioning, snapshots and replay infrastructure would compete with the
  saga and MLOps learning goals; the ledger already teaches the core idea of an
  append-only truth plus derived views.

## Open questions (to revisit)
- Should holds also be recorded in the ledger (modelling available vs booked balance)?
- When reconciliation finds drift: automatic repair or alert plus human decision?
