package com.hajime.coaching.application;

import com.hajime.coaching.*;
import com.hajime.coaching.domain.*;
import com.hajime.identity.CurrentUser;
import com.hajime.organization.*;
import com.hajime.practitioner.Practitioners;
import com.hajime.shared.*;
import com.hajime.training.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CoachingService {
  public record ReviewDetail(
      TrainingReview review, TrainingView training, List<Feedback> feedback) {}

  public record FeedbackResult(Feedback feedback, Goal goal) {}

  private final CoachingRepository repository;
  private final CurrentUser user;
  private final OrganizationAccess access;
  private final Practitioners practitioners;
  private final GoalAssignment goalAssignment;
  private final Clock clock;
  private final TrainingAccess training;
  private final ApplicationEventPublisher events;

  public CoachingService(
      CoachingRepository repository,
      CurrentUser user,
      OrganizationAccess access,
      Practitioners practitioners,
      GoalAssignment goalAssignment,
      Clock clock,
      TrainingAccess training,
      ApplicationEventPublisher events) {
    this.repository = repository;
    this.user = user;
    this.access = access;
    this.practitioners = practitioners;
    this.goalAssignment = goalAssignment;
    this.clock = clock;
    this.training = training;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public PageResult<TrainingReview> reviews(UUID org, String status, int page, int size) {
    access.requirePermission(org, user.id(), Permission.REVIEW_TRAINING);
    PageResult.limit(page, size);
    if (!Set.of("PENDING", "REVIEWED").contains(status))
      throw new ApiException(400, "INVALID_STATUS", "Review status must be PENDING or REVIEWED.");
    return repository.reviews(org, status, page, size);
  }

  @Transactional(readOnly = true)
  public ReviewDetail review(UUID org, UUID id) {
    access.requirePermission(org, user.id(), Permission.REVIEW_TRAINING);
    var review = requireReview(org, id);
    return new ReviewDetail(
        review,
        training.sharedInOrganization(review.trainingSessionId(), org),
        repository.feedbackForReview(org, id));
  }

  @Transactional
  public FeedbackResult feedback(UUID org, UUID reviewId, SubmitFeedback command) {
    var actor = user.id();
    access.requirePermission(org, actor, Permission.REVIEW_TRAINING);
    var before = requireReview(org, reviewId);
    if (before.version() != command.version())
      throw new ApiException(
          409, "CONCURRENT_MODIFICATION", "Reload the review before submitting feedback.");
    var now = clock.instant();
    repository.complete(before, before.reviewed(now));
    var feedback = Feedback.create(before, actor, command.body(), now);
    repository.addFeedback(feedback);
    training.reviewed(before.trainingSessionId(), org);
    events.publishEvent(
        new FeedbackCreated(
            feedback.id(),
            feedback.trainingSessionId(),
            feedback.practitionerProfileId(),
            org,
            actor));
    Goal goal = null;
    if (command.goal() != null) {
      var g = command.goal();
      if (g.sourceTrainingSessionId() != null
          && !g.sourceTrainingSessionId().equals(before.trainingSessionId()))
        throw new ApiException(
            400, "GOAL_TRAINING_MISMATCH", "A review goal must reference this training session.");
      goal =
          goalAssignment.assign(
              org,
              before.practitionerProfileId(),
              new AssignGoal(
                  before.trainingSessionId(),
                  g.relatedTechniqueId(),
                  g.title(),
                  g.objective(),
                  g.coachObservation(),
                  g.priority(),
                  g.reviewDate()),
              actor);
    }
    return new FeedbackResult(feedback, goal);
  }

  @Transactional
  public Goal assign(UUID org, UUID practitionerId, AssignGoal command) {
    return goalAssignment.assign(org, practitionerId, command, user.id());
  }

  @Transactional(readOnly = true)
  public PageResult<Feedback> feedback(UUID session, int page, int size) {
    PageResult.limit(page, size);
    return repository.feedbackForAthlete(profiles(), session, page, size);
  }

  @Transactional(readOnly = true)
  public PageResult<Goal> goals(Goal.Status status, int page, int size) {
    PageResult.limit(page, size);
    return repository.goals(profiles(), status, page, size);
  }

  @Transactional(readOnly = true)
  public Goal goal(UUID id) {
    return repository
        .goal(profiles(), id)
        .orElseThrow(() -> ApiException.missing("GOAL_NOT_FOUND"));
  }

  private TrainingReview requireReview(UUID org, UUID id) {
    return repository.review(org, id).orElseThrow(() -> ApiException.missing("REVIEW_NOT_FOUND"));
  }

  private List<UUID> profiles() {
    return practitioners.ownedBy(user.id()).stream().map(Practitioners.Profile::id).toList();
  }
}
