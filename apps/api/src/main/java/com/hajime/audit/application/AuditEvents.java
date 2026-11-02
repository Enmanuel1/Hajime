package com.hajime.audit.application;

import com.hajime.audit.AuditTrail;
import com.hajime.coaching.*;
import com.hajime.training.TrainingSessionShared;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AuditEvents {
  private final AuditTrail audit;

  public AuditEvents(AuditTrail audit) {
    this.audit = audit;
  }

  @EventListener
  public void shared(TrainingSessionShared e) {
    audit.record(
        e.actorUserId(),
        e.organizationId(),
        "TRAINING_SHARED",
        "TrainingSession",
        e.trainingSessionId(),
        Map.of());
  }

  @EventListener
  public void feedback(FeedbackCreated e) {
    audit.record(
        e.actorUserId(),
        e.organizationId(),
        "FEEDBACK_CREATED",
        "Feedback",
        e.feedbackId(),
        Map.of("trainingSessionId", e.trainingSessionId().toString()));
  }

  @EventListener
  public void goal(GoalAssigned e) {
    audit.record(
        e.actorUserId(),
        e.organizationId(),
        "GOAL_ASSIGNED",
        "Goal",
        e.goalId(),
        Map.of("practitionerProfileId", e.practitionerProfileId().toString()));
  }

  @EventListener
  public void organization(com.hajime.organization.OrganizationChanged e) {
    audit.record(
        e.actorUserId(), e.organizationId(), e.action(), e.entityType(), e.entityId(), Map.of());
  }

  @EventListener
  public void goalStatus(GoalStatusChanged e) {
    audit.record(
        e.actorUserId(), e.organizationId(), "GOAL_" + e.status(), "Goal", e.goalId(), Map.of());
  }

  @EventListener
  public void security(com.hajime.identity.AccountSecurityChanged e) {
    audit.record(e.userId(), null, e.action(), "Account", e.userId(), Map.of());
  }
}
