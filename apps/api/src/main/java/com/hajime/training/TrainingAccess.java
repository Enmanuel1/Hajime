package com.hajime.training;

import com.hajime.shared.BusinessException;
import com.hajime.training.application.TrainingViews;
import com.hajime.training.domain.TrainingRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organization-scoped contract for coaching. Never exposes persistence objects. */
@Service
public class TrainingAccess {
  private final TrainingRepository repository;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public TrainingAccess(
      TrainingRepository repository, ApplicationEventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public TrainingView sharedInOrganization(UUID id, UUID organization) {
    var d =
        repository
            .inOrganization(id, organization)
            .orElseThrow(() -> BusinessException.missing("TRAINING_NOT_FOUND"));
    if (!d.isShared()) throw BusinessException.missing("TRAINING_NOT_FOUND");
    return TrainingViews.from(d);
  }

  @Transactional
  public void reviewed(UUID id, UUID organization) {
    var session =
        repository
            .inOrganization(id, organization)
            .orElseThrow(() -> BusinessException.missing("TRAINING_NOT_FOUND"));
    repository.save(session.review(clock.instant()));
    events.publishEvent(new TrainingReviewed(id, organization));
  }
}
