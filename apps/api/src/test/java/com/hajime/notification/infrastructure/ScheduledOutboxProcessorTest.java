package com.hajime.notification.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hajime.notification.NotificationDelivery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class ScheduledOutboxProcessorTest {
  @Test
  void acknowledgementFailureIsNotReportedAsDeliveryFailureOrSuccess() {
    var store = mock(OutboxStore.class);
    var delivery = mock(NotificationDelivery.class);
    var claim = new OutboxStore.Claim(UUID.randomUUID(), UUID.randomUUID(), "TEST", "{}", 1);
    when(store.claim()).thenReturn(Optional.of(claim));
    when(store.acknowledge(claim)).thenThrow(new IllegalStateException("Database unavailable"));
    var metrics = new SimpleMeterRegistry();
    var settings =
        new OutboxSettings(
            1,
            8,
            4,
            Duration.ofSeconds(60),
            Duration.ofSeconds(10),
            Duration.ofSeconds(2),
            Duration.ofHours(1));
    var processor = new ScheduledOutboxProcessor(store, delivery, metrics, settings);
    try {
      assertThatThrownBy(processor::processBatch)
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Database unavailable");
      verify(delivery).deliver(claim.id(), claim.eventType(), claim.payload());
      verify(store, never()).fail(any(), anyString());
      assertThat(metrics.find("hajime.outbox.delivered").counter()).isNull();
      assertThat(metrics.find("hajime.outbox.failed").counter()).isNull();
    } finally {
      processor.stop();
      metrics.close();
    }
  }
}
