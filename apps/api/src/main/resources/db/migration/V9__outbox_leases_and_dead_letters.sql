ALTER TABLE outbox_event ADD COLUMN claim_token uuid;
ALTER TABLE outbox_event ADD COLUMN lease_until timestamptz;
ALTER TABLE outbox_event ADD COLUMN dead_lettered_at timestamptz;
ALTER TABLE outbox_event ADD CONSTRAINT outbox_claim_complete CHECK((claim_token IS NULL)=(lease_until IS NULL));
ALTER TABLE outbox_event ADD CONSTRAINT outbox_terminal_exclusive CHECK(processed_at IS NULL OR dead_lettered_at IS NULL);
ALTER TABLE outbox_event ADD CONSTRAINT outbox_terminal_unclaimed CHECK((processed_at IS NULL AND dead_lettered_at IS NULL) OR claim_token IS NULL);
DROP INDEX outbox_pending;
CREATE INDEX outbox_pending ON outbox_event(next_attempt_at,created_at,id) WHERE processed_at IS NULL AND dead_lettered_at IS NULL;
CREATE INDEX outbox_dead_letters ON outbox_event(dead_lettered_at,id) WHERE dead_lettered_at IS NOT NULL;
-- Retained independently from outbox cleanup so redelivery does not erase deduplication history.
CREATE TABLE local_notification_receipt(event_id uuid PRIMARY KEY,event_type varchar(60) NOT NULL,delivered_at timestamptz NOT NULL DEFAULT now());
