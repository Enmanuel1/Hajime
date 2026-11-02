package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CoachingApiTest extends ApiTest {
  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.jdbc.core.simple.JdbcClient db;

  long count(String table, UUID id) {
    return db.sql("select count(*) from " + table + " where entity_id=:id")
        .param("id", id)
        .query(Long.class)
        .single();
  }

  String reviewPath() {
    return "/api/v1/organizations/" + DOJO + "/training-reviews";
  }

  JsonNode shareAndFindReview(UUID session) throws Exception {
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(session), 201);
    ok(
        "athlete",
        "POST",
        "/api/v1/me/training-sessions/" + session + "/share",
        Map.of("version", 0),
        200);
    var list = ok("sensei", "GET", reviewPath() + "?size=100", null, 200).path("content");
    for (var review : list)
      if (review.path("trainingSessionId").asText().equals(session.toString())) return review;
    throw new AssertionError("Shared training did not enter the pending review queue");
  }

  @Test
  void completeAthleteCoachFeedbackGoalSlice() throws Exception {
    var session = UUID.randomUUID();
    var review = shareAndFindReview(session);
    String reviewId = review.path("id").asText();
    var detail = ok("sensei", "GET", reviewPath() + "/" + reviewId, null, 200);
    assertThat(detail.path("training").path("reflection").asText()).contains("sleeve grip");
    var result =
        ok(
            "sensei",
            "POST",
            reviewPath() + "/" + reviewId + "/feedback",
            Map.of(
                "version",
                0,
                "body",
                "Keep your supporting foot close before the O-soto-gari reap.",
                "goal",
                Map.of(
                    "title",
                    "Control the O-soto-gari entry",
                    "objective",
                    "Demonstrate a balanced entry in three supervised sessions.",
                    "priority",
                    "HIGH",
                    "relatedTechniqueId",
                    TECHNIQUE)),
            201);
    String goalId = result.path("goal").path("id").asText();
    assertThat(count("audit_log", session)).isEqualTo(1);
    assertThat(count("audit_log", UUID.fromString(goalId))).isEqualTo(1);
    assertThat(
            db.sql(
                    "select count(*) from outbox_event where aggregate_id=:id and event_type='GOAL_ASSIGNED'")
                .param("id", UUID.fromString(goalId))
                .query(Long.class)
                .single())
        .isEqualTo(1);

    assertThat(
            ok("athlete", "GET", "/api/v1/me/training-sessions/" + session, null, 200)
                .path("status")
                .asText())
        .isEqualTo("REVIEWED");
    assertThat(
            ok("athlete", "GET", "/api/v1/me/feedback?trainingSessionId=" + session, null, 200)
                .path("content")
                .get(0)
                .path("body")
                .asText())
        .contains("supporting foot");
    assertThat(
            ok("athlete", "GET", "/api/v1/me/goals/" + goalId, null, 200).path("source").asText())
        .isEqualTo("ORGANIZATION");
    assertThat(ok("athlete", "GET", "/api/v1/me/goals", null, 200).path("totalElements").asLong())
        .isPositive();
    assertThat(
            ok("sensei", "GET", reviewPath() + "/" + reviewId, null, 200)
                .path("review")
                .path("status")
                .asText())
        .isEqualTo("REVIEWED");
    assertThat(
            call(
                    "sensei",
                    "POST",
                    reviewPath() + "/" + reviewId + "/feedback",
                    Map.of("version", 0, "body", "Duplicate"))
                .status())
        .isEqualTo(409);
    assertThat(call("outsider", "GET", "/api/v1/me/goals/" + goalId, null).status()).isEqualTo(404);
    assertThat(call("athlete", "GET", reviewPath(), null).status()).isEqualTo(403);
    assertThat(call("outsider", "GET", reviewPath() + "/" + reviewId, null).status())
        .isEqualTo(403);
    assertThat(
            call(
                    "sensei",
                    "GET",
                    "/api/v1/organizations/" + OTHER_DOJO + "/training-reviews/" + reviewId,
                    null)
                .status())
        .isEqualTo(403);
    assertThat(
            call(
                    "outsider",
                    "GET",
                    "/api/v1/organizations/" + OTHER_DOJO + "/training-reviews/" + reviewId,
                    null)
                .status())
        .isEqualTo(404);
  }

  @Test
  void invalidOptionalGoalRollsBackFeedbackAndReview() throws Exception {
    var session = UUID.randomUUID();
    var review = shareAndFindReview(session);
    String path = reviewPath() + "/" + review.path("id").asText();
    var goal =
        Map.of(
            "title",
            "Entry practice",
            "objective",
            "Keep balance",
            "priority",
            "NORMAL",
            "relatedTechniqueId",
            UUID.randomUUID());
    assertThat(
            call(
                    "sensei",
                    "POST",
                    path + "/feedback",
                    Map.of("version", 0, "body", "Useful feedback", "goal", goal))
                .status())
        .isEqualTo(400);
    var detail = ok("sensei", "GET", path, null, 200);
    assertThat(detail.path("review").path("status").asText()).isEqualTo("PENDING");
    assertThat(detail.path("feedback").size()).isZero();
    assertThat(
            db.sql(
                    "select count(*) from outbox_event where event_type='FEEDBACK_CREATED' and payload->>'trainingSessionId'=:id")
                .param("id", session.toString())
                .query(Long.class)
                .single())
        .isZero();
    assertThat(
            db.sql(
                    "select count(*) from audit_log where action='FEEDBACK_CREATED' and metadata->>'trainingSessionId'=:id")
                .param("id", session.toString())
                .query(Long.class)
                .single())
        .isZero();
    assertThat(
            ok("athlete", "GET", "/api/v1/me/training-sessions/" + session, null, 200)
                .path("status")
                .asText())
        .isEqualTo("SHARED_WITH_COACH");
  }

  @Test
  void coachMayAssignGoalWithoutReviewButMustShareOrganizationWithAthlete() throws Exception {
    var command =
        Map.of(
            "title",
            "Safe side breakfalls",
            "objective",
            "Demonstrate Yoko-ukemi safely on both sides.",
            "priority",
            "NORMAL");
    String path = "/api/v1/organizations/" + DOJO + "/practitioners/" + PROFILE + "/goals";
    assertThat(ok("sensei", "POST", path, command, 201).path("status").asText())
        .isEqualTo("ACTIVE");
    assertThat(call("athlete", "POST", path, command).status()).isEqualTo(403);
    assertThat(
            call(
                    "outsider",
                    "POST",
                    "/api/v1/organizations/" + OTHER_DOJO + "/practitioners/" + PROFILE + "/goals",
                    command)
                .status())
        .isEqualTo(403);
  }

  @Test
  void concurrentReviewCompletionCommitsExactlyOneFeedback() throws Exception {
    var session = UUID.randomUUID();
    var review = shareAndFindReview(session);
    var path = reviewPath() + "/" + review.path("id").asText() + "/feedback";
    var membership = UUID.randomUUID();
    db.sql(
            "insert into organization_membership(id,organization_id,user_id,role) values(:id,:org,'20000000-0000-0000-0000-000000000003','INSTRUCTOR')")
        .param("id", membership)
        .param("org", DOJO)
        .update();
    var start = new java.util.concurrent.CyclicBarrier(2);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      java.util.function.Function<String, java.util.concurrent.Callable<Integer>> submit =
          actor ->
              () -> {
                start.await(10, java.util.concurrent.TimeUnit.SECONDS);
                return call(actor, "POST", path, Map.of("version", 0, "body", "Keep balance"))
                    .status();
              };
      var a = executor.submit(submit.apply("sensei"));
      var b = executor.submit(submit.apply("outsider"));
      assertThat(
              List.of(
                  a.get(20, java.util.concurrent.TimeUnit.SECONDS),
                  b.get(20, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    } finally {
      db.sql("delete from organization_membership where id=:id").param("id", membership).update();
    }
    assertThat(
            db.sql("select count(*) from feedback where training_session_id=:id")
                .param("id", session)
                .query(Long.class)
                .single())
        .isEqualTo(1);
    assertThat(
            db.sql(
                    "select count(*) from outbox_event where event_type='FEEDBACK_CREATED' and payload->>'trainingSessionId'=:id")
                .param("id", session.toString())
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }
}
