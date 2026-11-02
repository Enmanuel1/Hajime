package com.hajime.training.domain;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.ApiException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrainingInvariantsTest {
  @Test
  void internalCallersCannotCreateInvalidEntriesOrFutureTraining() {
    assertThatThrownBy(() -> new TrainingContent.TechniqueEntry(UUID.randomUUID(), 0, null, null))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> new TrainingContent.SparringEntry(101, 5, null))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                new TrainingContent(
                    Instant.now(),
                    60,
                    TrainingSession.PracticeType.MIXED,
                    null,
                    null,
                    null,
                    Arrays.asList((TrainingContent.TechniqueEntry) null),
                    List.of()))
        .isInstanceOf(ApiException.class);
    var now = Instant.parse("2026-11-01T12:00:00Z");
    var future =
        new TrainingContent(
            now.plusSeconds(1),
            60,
            TrainingSession.PracticeType.MIXED,
            null,
            null,
            null,
            List.of(),
            List.of());
    var id = UUID.randomUUID();
    assertThatThrownBy(() -> TrainingSession.record(id, id, id, null, future, now))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void recordingFactoryOwnsInitialStateAndDoesNotRetainMutableCollections() {
    var entries = new ArrayList<TrainingContent.TechniqueEntry>();
    entries.add(new TrainingContent.TechniqueEntry(UUID.randomUUID(), 2, null, null));
    var now = Instant.now();
    var id = UUID.randomUUID();
    var content =
        new TrainingContent(
            now, 60, TrainingSession.PracticeType.MIXED, null, null, null, entries, List.of());
    var session = TrainingSession.record(id, id, id, id, content, now);
    entries.clear();
    assertThat(session.snapshot().content().techniques()).hasSize(1);
    assertThat(session.snapshot().status()).isEqualTo(TrainingSession.Status.RECORDED);
    assertThat(session.share(now).snapshot().content()).isSameAs(content);
    assertThat(session.snapshot().status()).isEqualTo(TrainingSession.Status.RECORDED);
  }
}
