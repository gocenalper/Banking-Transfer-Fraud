-- Optimistic locking counter (see ADR-0004): Hibernate bumps this on every UPDATE and
-- adds "AND version = ?" to the WHERE clause, so a write based on a stale read matches
-- zero rows and fails instead of silently overwriting a concurrent change.
-- DEFAULT 0 matters: existing rows (created before this migration) must get a valid
-- starting value, otherwise the NOT NULL constraint would reject the migration itself.
ALTER TABLE account
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
