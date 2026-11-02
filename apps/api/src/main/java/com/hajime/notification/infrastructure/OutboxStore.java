package com.hajime.notification.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

/** Short, independently committed transactions; external delivery never holds a DB lock. */
@Repository
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class OutboxStore {
  public record Claim(UUID id, UUID token, String eventType, String payload, int attempts) {}

  private final JdbcClient db;
  private final OutboxSettings settings;

  public OutboxStore(JdbcClient db, OutboxSettings settings) {
    this.db = db;
    this.settings = settings;
  }

  public Optional<Claim> claim() {
    // An exhausted attempt may have crashed before recording its outcome.
    db.sql(
            """
        update outbox_event set dead_lettered_at=clock_timestamp(),claim_token=null,lease_until=null,
          last_error=coalesce(last_error,'LeaseExpired')
        where id in (select id from outbox_event where processed_at is null and dead_lettered_at is null
          and attempts>=:max and (lease_until is null or lease_until<=clock_timestamp())
          order by next_attempt_at,id limit :batch for update skip locked)
        """)
        .param("max", settings.maxAttempts())
        .param("batch", settings.batchSize())
        .update();
    var token = UUID.randomUUID();
    return db.sql(
            """
        with candidate as (
          select id from outbox_event
          where processed_at is null and dead_lettered_at is null and attempts<:max
            and next_attempt_at<=clock_timestamp() and (lease_until is null or lease_until<=clock_timestamp())
          order by next_attempt_at,created_at,id limit 1 for update skip locked
        )
        update outbox_event e set claim_token=:token,
          lease_until=clock_timestamp()+(:lease * interval '1 millisecond'),attempts=e.attempts+1
        from candidate c where e.id=c.id
        returning e.id,e.claim_token as token,e.event_type,e.payload::text as payload,e.attempts
        """)
        .param("max", settings.maxAttempts())
        .param("token", token)
        .param("lease", settings.leaseDuration().toMillis())
        .query(Claim.class)
        .optional();
  }

  public boolean acknowledge(Claim claim) {
    return db.sql(
                "update outbox_event set processed_at=clock_timestamp(),claim_token=null,lease_until=null,last_error=null where id=:id and claim_token=:token and processed_at is null and dead_lettered_at is null")
            .param("id", claim.id())
            .param("token", claim.token())
            .update()
        == 1;
  }

  public boolean fail(Claim claim, String error) {
    return db.sql(
                """
        update outbox_event set claim_token=null,lease_until=null,last_error=:error,
          dead_lettered_at=case when attempts>=:max then clock_timestamp() else null end,
          next_attempt_at=clock_timestamp()+(:delay * interval '1 millisecond')
        where id=:id and claim_token=:token and processed_at is null and dead_lettered_at is null
        """)
            .param("id", claim.id())
            .param("token", claim.token())
            .param("error", error)
            .param("max", settings.maxAttempts())
            .param("delay", settings.backoff(claim.attempts()).toMillis())
            .update()
        == 1;
  }

  public boolean requeue(UUID id) {
    return db.sql(
                "update outbox_event set dead_lettered_at=null,attempts=0,last_error=null,next_attempt_at=clock_timestamp() where id=:id and dead_lettered_at is not null and claim_token is null")
            .param("id", id)
            .update()
        == 1;
  }

  public int purgeProcessed(Instant before, int limit) {
    return db.sql(
            "delete from outbox_event where id in (select id from outbox_event where processed_at<:before order by processed_at,id limit :limit for update skip locked)")
        .param("before", Timestamp.from(before))
        .param("limit", limit)
        .update();
  }
}
