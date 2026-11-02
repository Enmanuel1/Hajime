# Outbox operations without paid services

The current implementation needs only the project's PostgreSQL database. No AWS, broker or external
notification service is configured. The existing dev profile enables `hajime.outbox.local-enabled=true`.
Outside dev/test a real NotificationDelivery adapter is required before opting in; local receipt delivery
is not an authentication system, push provider or user-facing inbox.

## Limits

```yaml
hajime:
  outbox:
    batch-size: 50
    max-attempts: 8
    max-concurrent-deliveries: 4
    lease-duration: PT60S
    delivery-timeout: PT10S
    initial-backoff: PT2S
    max-backoff: PT1H
```

Settings fail fast on invalid values. Lease duration must be at least twice the delivery deadline;
positive durations are bounded at seven days, batch size at 100, attempts/concurrent calls at 100.
Claiming increments attempts, including attempts interrupted by a process crash. Acknowledgment and
failure updates require the current claim token. An expired final attempt becomes a dead letter on the
next scan. No automatic deletion or requeue is enabled.

Providers must deduplicate side effects by event UUID, enforce their own I/O timeouts and respond to
interruption. Timeout cancellation cannot guarantee that a remote operation did not finish. A provider
ignoring cancellation retains its concurrency slot until it exits: processing may pause at the configured
limit, rather than accumulating unlimited calls. A restarted process recovers expired claims; at-least-once
semantics still require destination deduplication. No exactly-once external delivery is claimed.

## Inspect failures

Use a trusted database/operator connection, not a public endpoint:

```sql
SELECT id, event_type, organization_id, attempts, last_error, dead_lettered_at
FROM outbox_event
WHERE dead_lettered_at IS NOT NULL
ORDER BY dead_lettered_at, id
LIMIT 100;
```

Only exception class names or controlled failure labels are stored, not provider messages, payloads or
tokens. `DeliveryTimeout` identifies a processor deadline. Metrics `hajime.outbox.delivered` and
`hajime.outbox.failed` represent acknowledged outcomes after their database commits; terminal queue
state should be inspected directly. A crash between commit and metric update can undercount metrics.

## Requeue after fixing the cause

`OutboxMaintenance.requeue(eventId)` requeues only dead letters; it resets the attempt counter and
failure marker but preserves the event UUID and payload. The equivalent guarded operator SQL is:

```sql
UPDATE outbox_event
SET dead_lettered_at = NULL, attempts = 0, last_error = NULL,
    next_attempt_at = clock_timestamp()
WHERE id = :event_id AND dead_lettered_at IS NOT NULL AND claim_token IS NULL;
```

Check the affected row count. Do not regenerate event UUIDs to bypass destination deduplication.
These are internal trusted-operator operations with no HTTP exposure; adding administrative endpoints
later requires explicit authentication/authorization and audit.

## Retention

`OutboxMaintenance.purgeProcessed(before, limit)` deletes at most 1,000 acknowledged rows per call,
with a nonfuture cutoff. It never deletes pending, leased or dead-letter rows. Decide a retention period
before invoking it. Local receipt IDs remain independently stored so outbox cleanup cannot erase
previous deduplication. Receipt retention needs an explicit replay/deduplication window before deletion;
there is no automatic receipt purge.

V9 adds lease/dead-letter columns and the local receipt table without changing prior migrations. It has
been exercised in temporary PostgreSQL tests; this task does not migrate an existing development database.
