package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "hajime.auth.max-ip-attempts=1000")
class MvpLifecycleApiTest extends ProductionApiSupport {
  String dojo(String token) throws Exception {
    return request(
            "POST",
            "/api/v1/organizations",
            Map.of("name", "MVP dojo", "martialArtCode", "JUDO"),
            token)
        .body()
        .path("id")
        .asText();
  }

  JsonNode training(String token, String org) throws Exception {
    var p =
        request(
            "POST", "/api/v1/me/practitioner-profiles", Map.of("martialArtCode", "JUDO"), token);
    var b =
        new HashMap<String, Object>(
            Map.of(
                "practitionerProfileId",
                p.body().path("id").asText(),
                "occurredAt",
                "2026-01-01T12:00:00Z",
                "durationMinutes",
                60,
                "practiceType",
                "TECHNICAL",
                "techniques",
                List.of(),
                "sparring",
                List.of()));
    if (org != null) b.put("organizationId", org);
    var result = request("POST", "/api/v1/me/training-sessions", b, token);
    assertThat(result.status()).isEqualTo(201);
    return result.body();
  }

  Map<String, Object> update(long version, String org) {
    var b =
        new HashMap<String, Object>(
            Map.of(
                "version",
                version,
                "occurredAt",
                "2026-01-01T12:00:00Z",
                "durationMinutes",
                75,
                "practiceType",
                "MIXED",
                "reflection",
                "Corrected entry",
                "techniques",
                List.of(),
                "sparring",
                List.of()));
    if (org != null) b.put("organizationId", org);
    return b;
  }

  @Test
  void ownerEditsUnsharedTrainingWithVersionAndCannotChangeProvenance() throws Exception {
    var token = access(register(email()));
    var org = dojo(token);
    var before = training(token, null);
    var path = "/api/v1/me/training-sessions/" + before.path("id").asText();
    var body = update(0, org);
    var edited = request("PUT", path, body, token);
    assertThat(edited.status()).isEqualTo(200);
    assertThat(edited.body().path("durationMinutes").asInt()).isEqualTo(75);
    assertThat(edited.body().path("version").asLong()).isEqualTo(1);
    for (var field : List.of("id", "practitionerProfileId", "source", "createdAt"))
      assertThat(edited.body().path(field)).isEqualTo(before.path(field));
    assertThat(request("PUT", path, body, token).status()).isEqualTo(409);
    var foreign = access(register(email()));
    assertThat(request("PUT", path, update(1, org), foreign).status()).isEqualTo(404);
    body = update(1, org);
    body.put("source", "OFFICIAL");
    assertThat(request("PUT", path, body, token).status()).isEqualTo(400);
    body = update(1, org);
    body.remove("version");
    assertThat(request("PUT", path, body, token).status()).isEqualTo(400);
    assertThat(request("POST", path + "/share", Map.of("version", 1), token).status())
        .isEqualTo(200);
    assertThat(request("PUT", path, update(2, org), token).status()).isEqualTo(409);
  }

  @Test
  void editingAndSharingTheSameVersionCannotBothSucceed() throws Exception {
    var token = access(register(email()));
    var org = dojo(token);
    var session = training(token, org);
    var path = "/api/v1/me/training-sessions/" + session.path("id").asText();
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var edit =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request("PUT", path, update(0, org), token);
              });
      var share =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request("POST", path + "/share", Map.of("version", 0), token);
              });
      assertThat(
              List.of(
                  edit.get(20, TimeUnit.SECONDS).status(),
                  share.get(20, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(200, 409);
    }
    var after = request("GET", path, null, token).body();
    assertThat(after.path("version").asLong()).isEqualTo(1);
    var count =
        db.sql("select count(*) from training_review where training_session_id=:id")
            .param("id", UUID.fromString(session.path("id").asText()))
            .query(Long.class)
            .single();
    assertThat(count).isEqualTo(after.path("status").asText().equals("SHARED_WITH_COACH") ? 1 : 0);
  }

  JsonNode goal(String token, String org, String profile) throws Exception {
    var result =
        request(
            "POST",
            "/api/v1/organizations/" + org + "/practitioners/" + profile + "/goals",
            Map.of(
                "title",
                "Improve posture",
                "objective",
                "Practice with coach",
                "priority",
                "NORMAL"),
            token);
    assertThat(result.status()).isEqualTo(201);
    return result.body();
  }

  @Test
  void coachClosesGoalsWithScopeAndVersionAndAthleteSeesStatus() throws Exception {
    var token = access(register(email()));
    var org = dojo(token);
    var session = training(token, org);
    var goal = goal(token, org, session.path("practitionerProfileId").asText());
    var path = "/api/v1/organizations/" + org + "/goals/" + goal.path("id").asText();
    var foreign = access(register(email()));
    var other = dojo(foreign);
    assertThat(request("POST", path + "/complete", Map.of("version", 0), foreign).status())
        .isEqualTo(403);
    assertThat(request("GET", path.replace(org, other), null, foreign).status()).isEqualTo(404);
    assertThat(request("POST", path + "/complete", Map.of("version", 9), token).status())
        .isEqualTo(409);
    var result = request("POST", path + "/complete", Map.of("version", 0), token);
    assertThat(result.status()).isEqualTo(200);
    assertThat(result.body().path("status").asText()).isEqualTo("ACHIEVED");
    assertThat(result.body().path("version").asLong()).isEqualTo(1);
    assertThat(request("POST", path + "/cancel", Map.of("version", 1), token).status())
        .isEqualTo(409);
    assertThat(
            request("GET", "/api/v1/me/goals?status=ACHIEVED", null, token)
                .body()
                .path("content")
                .toString())
        .contains(goal.path("id").asText());
    var cancelled = goal(token, org, session.path("practitionerProfileId").asText());
    assertThat(
            request(
                    "POST",
                    "/api/v1/organizations/"
                        + org
                        + "/goals/"
                        + cancelled.path("id").asText()
                        + "/cancel",
                    Map.of("version", 0),
                    token)
                .body()
                .path("status")
                .asText())
        .isEqualTo("CANCELLED");
    assertThat(
            db.sql("select count(*) from audit_log where entity_id=:id and action='GOAL_ACHIEVED'")
                .param("id", UUID.fromString(goal.path("id").asText()))
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void concurrentCompletionAndCancellationCommitOnlyOneTransition() throws Exception {
    var token = access(register(email()));
    var org = dojo(token);
    var session = training(token, org);
    var g = goal(token, org, session.path("practitionerProfileId").asText());
    var path = "/api/v1/organizations/" + org + "/goals/" + g.path("id").asText();
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var a =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request("POST", path + "/complete", Map.of("version", 0), token);
              });
      var b =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request("POST", path + "/cancel", Map.of("version", 0), token);
              });
      assertThat(
              List.of(a.get(20, TimeUnit.SECONDS).status(), b.get(20, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            db.sql(
                    "select count(*) from audit_log where entity_id=:id and action in ('GOAL_ACHIEVED','GOAL_CANCELLED')")
                .param("id", UUID.fromString(g.path("id").asText()))
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }
}
