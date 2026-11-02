package com.hajime.notification.infrastructure;

import com.hajime.coaching.*;
import com.hajime.training.TrainingSessionShared;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxWriter {
  private final JdbcClient db;
  private final ObjectMapper json;

  public OutboxWriter(JdbcClient db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  @EventListener
  public void shared(TrainingSessionShared e) {
    save("TRAINING_SHARED", e.trainingSessionId(), e.organizationId(), e);
  }

  @EventListener
  public void feedback(FeedbackCreated e) {
    save("FEEDBACK_CREATED", e.feedbackId(), e.organizationId(), e);
  }

  @EventListener
  public void goal(GoalAssigned e) {
    save("GOAL_ASSIGNED", e.goalId(), e.organizationId(), e);
  }

  private void save(String type, UUID aggregate, UUID org, Object payload) {
    db.sql(
            "insert into outbox_event(id,event_type,aggregate_id,organization_id,payload) values(:id,:type,:aggregate,:org,cast(:payload as jsonb))")
        .param("id", UUID.randomUUID())
        .param("type", type)
        .param("aggregate", aggregate)
        .param("org", org)
        .param("payload", json.writeValueAsString(payload))
        .update();
  }
}
