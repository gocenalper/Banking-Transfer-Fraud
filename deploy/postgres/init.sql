-- Database-per-service: one PostgreSQL container for local convenience, but every service
-- owns a separate database. In production these become separate instances; the boundary is identical.
CREATE DATABASE account_db;
CREATE DATABASE ledger_db;
CREATE DATABASE transfer_db;
