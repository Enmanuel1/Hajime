package com.hajime.notification.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("hajime.outbox")
public record OutboxSettings(
    @DefaultValue("50") int batchSize,
    @DefaultValue("8") int maxAttempts,
    @DefaultValue("4") int maxConcurrentDeliveries,
    @DefaultValue("PT60S") Duration leaseDuration,
    @DefaultValue("PT10S") Duration deliveryTimeout,
    @DefaultValue("PT2S") Duration initialBackoff,
    @DefaultValue("PT1H") Duration maxBackoff) {
  public OutboxSettings {
    if (maxConcurrentDeliveries < 1
        || maxConcurrentDeliveries > 100
        || batchSize < 1
        || batchSize > 100
        || maxAttempts < 1
        || maxAttempts > 100
        || !positive(leaseDuration)
        || !positive(deliveryTimeout)
        || !positive(initialBackoff)
        || !positive(maxBackoff)
        || leaseDuration.compareTo(deliveryTimeout.multipliedBy(2)) < 0
        || maxBackoff.compareTo(initialBackoff) < 0)
      throw new IllegalArgumentException(
          "Invalid outbox limits: lease must be at least twice the delivery timeout.");
  }

  private static boolean positive(Duration d) {
    return d != null && d.toMillis() >= 1 && d.compareTo(Duration.ofDays(7)) <= 0;
  }

  public Duration backoff(int attempts) {
    long millis = initialBackoff.toMillis();
    long cap = maxBackoff.toMillis();
    for (int i = 1; i < attempts && millis < cap; i++) millis = Math.min(cap, millis * 2);
    return Duration.ofMillis(millis);
  }
}
