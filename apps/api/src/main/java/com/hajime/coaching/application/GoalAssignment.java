package com.hajime.coaching.application;

import com.hajime.coaching.*;
import com.hajime.coaching.domain.*;
import com.hajime.martialart.Catalog;
import com.hajime.organization.*;
import com.hajime.practitioner.Practitioners;
import com.hajime.shared.ApiException;
import com.hajime.training.TrainingAccess;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Reused by standalone goals and feedback within the caller's transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class GoalAssignment {
  private final CoachingRepository repository;
  private final OrganizationAccess access;
  private final Practitioners practitioners;
  private final Catalog catalog;
  private final TrainingAccess training;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  GoalAssignment(
      CoachingRepository repository,
      OrganizationAccess access,
      Practitioners practitioners,
      Catalog catalog,
      TrainingAccess training,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.access = access;
    this.practitioners = practitioners;
    this.catalog = catalog;
    this.training = training;
    this.events = events;
    this.clock = clock;
  }

  public Goal assign(UUID org, UUID practitionerId, AssignGoal command, UUID actor) {
    access.requirePermission(org, actor, Permission.ASSIGN_GOAL);
    var profile = practitioners.require(practitionerId);
    access.requireMember(org, profile.userId());
    access.requireMartialArt(org, profile.martialArtId());
    if (command.relatedTechniqueId() != null)
      catalog.requireTechnique(profile.martialArtId(), command.relatedTechniqueId());
    if (command.sourceTrainingSessionId() != null) {
      var session = training.sharedInOrganization(command.sourceTrainingSessionId(), org);
      if (!session.practitionerProfileId().equals(profile.id()))
        throw new ApiException(
            400, "GOAL_TRAINING_MISMATCH", "Training must belong to the target practitioner.");
    }
    var now = clock.instant();
    var goal =
        Goal.assign(
            profile.id(),
            org,
            profile.martialArtId(),
            actor,
            command.sourceTrainingSessionId(),
            command.relatedTechniqueId(),
            command.title(),
            command.objective(),
            command.coachObservation(),
            command.priority(),
            command.reviewDate(),
            now);
    repository.addGoal(goal);
    events.publishEvent(new GoalAssigned(goal.id(), profile.id(), org, actor));
    return goal;
  }
}
