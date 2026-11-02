package com.hajime.notification.infrastructure;

import com.hajime.notification.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "hajime.outbox.local-enabled", havingValue = "true")
public class ScheduledOutboxProcessor implements OutboxProcessor {
  public record Pending(UUID id, String eventType, String payload, int attempts) {}

  private final JdbcClient db;
  private final NotificationDelivery delivery;
  private final MeterRegistry metrics;

  public ScheduledOutboxProcessor(
      JdbcClient db, NotificationDelivery delivery, MeterRegistry metrics) {
    this.db = db;
    this.delivery = delivery;
    this.metrics = metrics;
  }

  @Scheduled(
      fixedDelayString = "${hajime.outbox.delay-ms:5000}",
      initialDelayString = "${hajime.outbox.initial-delay-ms:5000}")
  @Transactional
  public int processBatch() {
    var pending =
        db.sql(
                "select id,event_type,payload::text as payload,attempts from outbox_event where processed_at is null and next_attempt_at<=now() order by created_at,id limit 50 for update skip locked")
            .query(Pending.class)
            .list();
    for (var event : pending) {
      try {
        delivery.deliver(event.id(), event.eventType(), event.payload());
        db.sql(
                "update outbox_event set processed_at=now(),attempts=attempts+1,last_error=null where id=:id")
            .param("id", event.id())
            .update();
        metrics.counter("hajime.outbox.delivered", "type", event.eventType()).increment();
      } catch (RuntimeException failure) {
        var retry =
            Instant.now().plusSeconds(Math.min(3600, 1L << Math.min(event.attempts() + 1, 12)));
        db.sql(
                "update outbox_event set attempts=attempts+1,next_attempt_at=:retry,last_error=:error where id=:id")
            .param("retry", Timestamp.from(retry))
            .param("error", failure.getClass().getSimpleName())
            .param("id", event.id())
            .update();
        metrics.counter("hajime.outbox.failed", "type", event.eventType()).increment();
      }
    }
    return pending.size();
  }
}
