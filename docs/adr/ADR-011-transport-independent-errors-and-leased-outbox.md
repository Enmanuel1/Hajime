# ADR-011: Transport-independent errors and leased outbox

Status: Accepted

## Context

Business rules were carrying HTTP numbers. Outbox delivery held database locks while performing a side
effect, retried forever, and had no recovery/retention contract. The project should remain free to run
locally with PostgreSQL and without cloud or a message broker.

## Decision

BusinessException contains a semantic category and stable code; the web adapter maps it to HTTP.
Preserve public response statuses/codes and guard the dependency direction with an architecture test.

Keep transactional event publication. Replace long delivery transactions with one-event claims committed
before delivery, expiring leases and random fencing tokens. Use bounded, timed delivery attempts and
capped retries with dead letters. A timed-out call retains an in-flight slot until the actual call exits.
Require provider I/O deadlines and destination idempotency. Do not interpret acknowledgment database
errors as provider errors. Increment outcome metrics only after committed state changes.

The development sink stores a durable receipt deduplicated by event UUID. Provide trusted internal
maintenance for requeue and bounded processed-event retention, with no public administrative endpoints.
Keep receipt deduplication history independent from outbox retention.

## Consequences

Recovery is at least once, not exactly once. Cancellation cannot reverse an accepted side effect.
Stale workers cannot acknowledge a newer claim. Exhausted events need deliberate operator action.
No AWS services, workflows, dependencies or paid providers are introduced. Local JWT authentication
is a separate scope; this decision does not change identity adapters or provision accounts.

This refines ADR-006's processing/retry mechanism while preserving PostgreSQL-first publication.
