package com.hajime.notification;

import java.util.UUID;

/**
 * At-least-once delivery: deduplicate the side effect using eventId, including concurrent
 * redelivery. Providers must enforce their own I/O timeouts below delivery-timeout and honor
 * interruption. A timeout/cancellation cannot undo a side effect already accepted by an external
 * provider.
 */
public interface NotificationDelivery {
  void deliver(UUID eventId, String eventType, String payload);
}
