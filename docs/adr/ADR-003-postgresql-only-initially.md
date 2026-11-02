# ADR-003: PostgreSQL only initially

Status: Accepted

## Context

The first slice needs relational integrity, transactions, ownership filtering, and a durable outbox. It has no concrete cache/search requirement.

## Decision

Use PostgreSQL, Flyway, normal indexed columns, and sparse JSONB. Do not add Redis, document stores, search engines, or separate databases per organization.

## Consequences

One backup/migration model and atomic business writes. Revisit additional infrastructure only with measured requirements.
