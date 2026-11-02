package com.hajime.notification.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxSettingsTest {
  @Test
  void retriesAreExponentialAndCapped() {
    var settings =
        new OutboxSettings(
            50,
            8,
            4,
            Duration.ofSeconds(60),
            Duration.ofSeconds(10),
            Duration.ofSeconds(2),
            Duration.ofHours(1));
    assertThat(settings.backoff(1)).isEqualTo(Duration.ofSeconds(2));
    assertThat(settings.backoff(3)).isEqualTo(Duration.ofSeconds(8));
    assertThat(settings.backoff(100)).isEqualTo(Duration.ofHours(1));
  }

  @Test
  void invalidTimeoutAndBatchSettingsFailBeforeProcessing() {
    assertThatThrownBy(
            () ->
                new OutboxSettings(
                    50,
                    8,
                    4,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(2),
                    Duration.ofHours(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new OutboxSettings(
                    0,
                    8,
                    4,
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(2),
                    Duration.ofHours(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
