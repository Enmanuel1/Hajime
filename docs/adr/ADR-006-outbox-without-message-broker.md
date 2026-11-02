# ADR-006: Outbox without message broker

Status: Accepted

## Context

Notifications must not be lost when a business action commits, and brokers are unnecessary for the initial slice.

## Decision

Persist minimal event payloads in the business transaction. Process bounded PostgreSQL batches with row locks and skip-locked, retry failed delivery, and expose replaceable delivery/processor ports. Enable only a local sink under dev/test.

## Consequences

Delivery is at least once, so external consumers must deduplicate event UUIDs. Production events remain pending until a real adapter/processor is deliberately enabled. Retention/dead-letter tooling is future work.
