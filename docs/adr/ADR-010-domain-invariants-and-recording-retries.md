# ADR-010: Domain invariants and recording retries

Status: Accepted

## Context

The mobile client can lose a creation response. Domain rules must also hold for future non-HTTP
callers, and HTTP representation changes must not define aggregate state.

## Decision

Use typed immutable TrainingSession snapshots and TrainingContent values, mapped to stable HTTP
views at the application boundary. Creation factories own initial state/provenance and validate
creation-time rules; reconstruction preserves historical dates. Keep current optimistic locking.
Goal/Feedback validate their own invariants; GoalAssignment is shared inside the caller's
transaction, and review creation has a dedicated event listener. Use an injected UTC Clock.

For a client-supplied training UUID, serialize creation through a PostgreSQL transaction advisory
lock keyed by its hash. Compare owner, scope and recorded content with any existing session.
Identical recordings return the current persisted session, preserving later state and using the
existing HTTP 201 contract. Other content/ownership conflicts return a specific error with no
stored data. Technical entries are order-insensitive; sparring is ordered. Normalize occurrence
timestamps to PostgreSQL microseconds. No UUID means no replay deduplication.

## Consequences

This remains one PostgreSQL transaction across API instances, without cache or process-local locks.
A hash collision can serialize unrelated UUIDs but cannot change correctness. Replays still pass
current authorization/catalog checks. This contract is for immutable recorded content: an edit API
will need a separate idempotency strategy. No general offline sync engine is introduced.
Composite foreign keys in V8 protect current training/coaching scope consistency. Migration will
fail on existing inconsistencies rather than silently correcting data. Production JWT policy now
requires Cognito access-token client_id/token_use alongside the decoder's standard validation.
