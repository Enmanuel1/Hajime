package com.hajime.notification;

import java.util.UUID;

/** Delivery is at least once. Implementations must deduplicate using eventId. */
public interface NotificationDelivery {
  void deliver(UUID eventId, String eventType, String payload);
}
