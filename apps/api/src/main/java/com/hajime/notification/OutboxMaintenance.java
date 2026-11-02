package com.hajime.notification;

import com.hajime.notification.infrastructure.OutboxStore;
import com.hajime.shared.DomainValidation;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Internal operator contract, intentionally not exposed as an HTTP endpoint. */
@Service
public class OutboxMaintenance {
  private final OutboxStore store;
  private final Clock clock;

  public OutboxMaintenance(OutboxStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public boolean requeue(UUID eventId) {
    return store.requeue(DomainValidation.required(eventId, "Event ID"));
  }

  public int purgeProcessed(Instant before, int limit) {
    DomainValidation.require(
        before != null && !before.isAfter(clock.instant()),
        "Retention cutoff cannot be in the future.");
    DomainValidation.require(limit >= 1 && limit <= 1000, "Retention batch must be 1..1000.");
    return store.purgeProcessed(before, limit);
  }
}
