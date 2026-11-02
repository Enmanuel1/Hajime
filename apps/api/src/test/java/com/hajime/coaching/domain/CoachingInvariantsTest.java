package com.hajime.coaching.domain;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.ApiException;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoachingInvariantsTest {
  private final UUID id = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-01T12:00:00Z");

  @Test
  void assignmentProtectsTextPriorityAndDateWithoutHttpValidation() {
    assertThatThrownBy(() -> assign(" ", Goal.Priority.NORMAL, null))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> assign("Practice", null, null)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> assign("Practice", Goal.Priority.NORMAL, LocalDate.of(2026, 10, 31)))
        .isInstanceOf(ApiException.class);
    assertThat(assign(" Practice ", Goal.Priority.NORMAL, LocalDate.of(2026, 11, 1)).title())
        .isEqualTo("Practice");
  }

  private Goal assign(String title, Goal.Priority priority, LocalDate date) {
    return Goal.assign(
        id, id, id, id, null, null, title, "Improve balance", null, priority, date, now);
  }

  @Test
  void feedbackCannotBeBlankWhenCreatedOutsideTheController() {
    var review =
        new TrainingReview(id, id, id, id, id, TrainingReview.Status.PENDING, now, null, 0);
    assertThatThrownBy(() -> Feedback.create(review, id, " ", now))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> Feedback.create(review, id, "x".repeat(4001), now))
        .isInstanceOf(ApiException.class);
  }
}
