package com.hajime.coaching.infrastructure;

import com.hajime.coaching.domain.*;
import com.hajime.shared.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SqlCoachingRepository implements CoachingRepository {
  private final JdbcClient db;

  public SqlCoachingRepository(JdbcClient db) {
    this.db = db;
  }

  public void addReview(TrainingReview r) {
    db.sql(
            "insert into training_review(id,training_session_id,practitioner_profile_id,organization_id,martial_art_id,status,created_at,version) values(:id,:session,:profile,:org,:art,'PENDING',:created,0)")
        .param("id", r.id())
        .param("session", r.trainingSessionId())
        .param("profile", r.practitionerProfileId())
        .param("org", r.organizationId())
        .param("art", r.martialArtId())
        .param("created", java.sql.Timestamp.from(r.createdAt()))
        .update();
  }

  public Optional<TrainingReview> review(UUID org, UUID id) {
    return db.sql("select * from training_review where organization_id=:org and id=:id")
        .param("org", org)
        .param("id", id)
        .query(TrainingReview.class)
        .optional();
  }

  public PageResult<TrainingReview> reviews(UUID org, String status, int page, int size) {
    var rows =
        db.sql(
                "select * from training_review where organization_id=:org and status=:status order by created_at,id limit :size offset :offset")
            .param("org", org)
            .param("status", status)
            .param("size", size)
            .param("offset", page * size)
            .query(TrainingReview.class)
            .list();
    var count =
        db.sql("select count(*) from training_review where organization_id=:org and status=:status")
            .param("org", org)
            .param("status", status)
            .query(Long.class)
            .single();
    return new PageResult<>(rows, page, size, count);
  }

  public void complete(TrainingReview before, TrainingReview after) {
    int changed =
        db.sql(
                "update training_review set status='REVIEWED',reviewed_at=:at,version=version+1 where id=:id and organization_id=:org and version=:version and status='PENDING'")
            .param("at", java.sql.Timestamp.from(after.reviewedAt()))
            .param("id", before.id())
            .param("org", before.organizationId())
            .param("version", before.version())
            .update();
    if (changed != 1)
      throw BusinessException.conflict(
          "CONCURRENT_MODIFICATION",
          "Another coach completed this review. Reload before retrying.");
  }

  public void addFeedback(Feedback f) {
    db.sql(
            "insert into feedback(id,review_id,training_session_id,practitioner_profile_id,author_user_id,organization_id,body,created_at) values(:id,:review,:session,:profile,:author,:org,:body,:at)")
        .param("id", f.id())
        .param("review", f.reviewId())
        .param("session", f.trainingSessionId())
        .param("profile", f.practitionerProfileId())
        .param("author", f.authorUserId())
        .param("org", f.organizationId())
        .param("body", f.body())
        .param("at", java.sql.Timestamp.from(f.createdAt()))
        .update();
  }

  public List<Feedback> feedbackForReview(UUID org, UUID review) {
    return db.sql(
            "select * from feedback where organization_id=:org and review_id=:review order by created_at,id")
        .param("org", org)
        .param("review", review)
        .query(Feedback.class)
        .list();
  }

  public PageResult<Feedback> feedbackForAthlete(
      List<UUID> profiles, UUID session, int page, int size) {
    if (profiles.isEmpty()) return new PageResult<>(List.of(), page, size, 0);
    String where =
        " where practitioner_profile_id in (:profiles)"
            + (session == null ? "" : " and training_session_id=:session");
    var rows =
        db.sql(
                "select * from feedback"
                    + where
                    + " order by created_at desc,id limit :size offset :offset")
            .param("profiles", profiles)
            .param("size", size)
            .param("offset", page * size);
    var count = db.sql("select count(*) from feedback" + where).param("profiles", profiles);
    if (session != null) {
      rows.param("session", session);
      count.param("session", session);
    }
    return new PageResult<>(
        rows.query(Feedback.class).list(), page, size, count.query(Long.class).single());
  }

  public void addGoal(Goal g) {
    db.sql(
            "insert into goal(id,practitioner_profile_id,organization_id,martial_art_id,assigned_by_user_id,source_training_session_id,related_technique_id,title,objective,coach_observation,priority,review_date,status,source,created_at,updated_at,version) values(:id,:profile,:org,:art,:author,:session,:technique,:title,:objective,:observation,:priority,:date,:status,:source,:created,:updated,0)")
        .param("id", g.id())
        .param("profile", g.practitionerProfileId())
        .param("org", g.organizationId())
        .param("art", g.martialArtId())
        .param("author", g.assignedByUserId())
        .param("session", g.sourceTrainingSessionId())
        .param("technique", g.relatedTechniqueId())
        .param("title", g.title())
        .param("objective", g.objective())
        .param("observation", g.coachObservation())
        .param("priority", g.priority().name())
        .param("date", g.reviewDate())
        .param("status", g.status().name())
        .param("source", g.source().name())
        .param("created", java.sql.Timestamp.from(g.createdAt()))
        .param("updated", java.sql.Timestamp.from(g.updatedAt()))
        .update();
  }

  public PageResult<Goal> goals(List<UUID> profiles, Goal.Status status, int page, int size) {
    if (profiles.isEmpty()) return new PageResult<>(List.of(), page, size, 0);
    var rows =
        db.sql(
                "select * from goal where practitioner_profile_id in (:profiles) and status=:status order by created_at desc,id limit :size offset :offset")
            .param("profiles", profiles)
            .param("status", status.name())
            .param("size", size)
            .param("offset", page * size)
            .query(Goal.class)
            .list();
    long count =
        db.sql(
                "select count(*) from goal where practitioner_profile_id in (:profiles) and status=:status")
            .param("profiles", profiles)
            .param("status", status.name())
            .query(Long.class)
            .single();
    return new PageResult<>(rows, page, size, count);
  }

  public Optional<Goal> goal(List<UUID> profiles, UUID id) {
    if (profiles.isEmpty()) return Optional.empty();
    return db.sql("select * from goal where id=:id and practitioner_profile_id in (:profiles)")
        .param("id", id)
        .param("profiles", profiles)
        .query(Goal.class)
        .optional();
  }

  public Optional<Goal> goalInOrganization(UUID organization, UUID id) {
    return db.sql("select * from goal where organization_id=:org and id=:id")
        .param("org", organization)
        .param("id", id)
        .query(Goal.class)
        .optional();
  }

  public Goal closeGoal(Goal before, Goal after) {
    return db.sql(
            "update goal set status=:status,updated_at=:at,version=version+1 where id=:id and organization_id=:org and version=:version and status='ACTIVE' returning *")
        .param("status", after.status().name())
        .param("at", java.sql.Timestamp.from(after.updatedAt()))
        .param("id", before.id())
        .param("org", before.organizationId())
        .param("version", before.version())
        .query(Goal.class)
        .optional()
        .orElseThrow(
            () ->
                BusinessException.conflict(
                    "CONCURRENT_MODIFICATION", "Goal changed. Reload before retrying."));
  }
}
