# ADR 0001 — Microservices from day one, in a monorepo, hexagonal per service

**Date:** 2026-08-14 · **Status:** Accepted (supersedes the modular-monolith alternative)

## Context
The primary goal of this project is to exercise the hard problems of distributed systems
first-hand: sagas, partial failure, idempotency, eventual consistency. A modular monolith
defers those problems; without a real network boundary the muscles never get trained.

## Decision
- Each bounded context is a separately deployable microservice: `account`, `ledger`,
  `transfer`, `notification` (Java/Spring Boot) plus `fraud` (Python/FastAPI).
- **Monorepo**: one repository, one Maven reactor — shared refactoring stays cheap while
  deployment remains independent per service.
- **Database-per-service**: locally a single PostgreSQL container but a separate database
  per service (`account_db`, `ledger_db`, `transfer_db`); the boundary is identical to
  production.
- The `common` library carries contracts only (Money, typed identifiers, event schemas) —
  business logic, entities or Spring dependencies are forbidden there.
- Inside every service the architecture is hexagonal; the layering is enforced per
  service by ArchUnit tests.
- Inter-service communication: synchronous REST for saga commands at first
  (transfer → account/ledger) plus Kafka events; saga orchestration lives in
  transfer-service.

## Consequences accepted deliberately
- The double-entry invariant is protected across services by **eventual consistency**;
  a reconciliation job becomes mandatory.
- A transactional outbox is needed in every writing service, not in one place.
- Operational overhead: per-service migrations, health checks and deployments.
