package com.hajime.training.domain;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.RecordSource;
import com.hajime.training.TrainingView;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrainingSessionTest {
  TrainingSession recorded(UUID org) {
    var now = Instant.now();
    return com.hajime.TrainingFixtures.restore(
        new TrainingView(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            org,
            now,
            60,
            "TECHNICAL",
            5,
            4,
            "Practice gripping",
            RecordSource.PERSONAL,
            "RECORDED",
            now,
            now,
            0L,
            List.of(),
            List.of()));
  }

  @Test
  void onlyRecordedTrainingWithOrganizationCanBeSharedAndReviewedOnce() {
    var session = recorded(UUID.randomUUID());
    var shared = session.share(Instant.now());
    assertThat(session.snapshot().status()).isEqualTo(TrainingSession.Status.RECORDED);
    assertThat(shared.review(Instant.now()).snapshot().status())
        .isEqualTo(TrainingSession.Status.REVIEWED);
    assertThatThrownBy(() -> shared.share(Instant.now())).hasMessageContaining("Only recorded");
    assertThatThrownBy(() -> session.review(Instant.now())).hasMessageContaining("Only shared");
    assertThatThrownBy(() -> recorded(null).share(Instant.now()))
        .hasMessageContaining("Choose an organization");
  }
}
