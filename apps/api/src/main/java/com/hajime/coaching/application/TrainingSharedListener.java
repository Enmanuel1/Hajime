package com.hajime.coaching.application;

import com.hajime.coaching.domain.*;
import com.hajime.training.TrainingSessionShared;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
class TrainingSharedListener {
  private final CoachingRepository repository;
  private final Clock clock;

  TrainingSharedListener(CoachingRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void shared(TrainingSessionShared event) {
    repository.addReview(
        new TrainingReview(
            UUID.randomUUID(),
            event.trainingSessionId(),
            event.practitionerProfileId(),
            event.organizationId(),
            event.martialArtId(),
            TrainingReview.Status.PENDING,
            clock.instant(),
            null,
            0));
  }
}
