package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class TrainingCreationApiTest extends ApiTest {
  @Autowired JdbcClient db;

  @Test
  void retryReturnsTheOriginalAndDoesNotResetSharedState() throws Exception {
    var id = UUID.randomUUID();
    var input = training(id);
    var first = ok("athlete", "POST", "/api/v1/me/training-sessions", input, 201);
    var repeated = ok("athlete", "POST", "/api/v1/me/training-sessions", input, 201);
    assertThat(repeated).isEqualTo(first);
    ok(
        "athlete",
        "POST",
        "/api/v1/me/training-sessions/" + id + "/share",
        Map.of("version", 0),
        200);
    var afterShare = ok("athlete", "POST", "/api/v1/me/training-sessions", input, 201);
    assertThat(afterShare.path("status").asText()).isEqualTo("SHARED_WITH_COACH");
    assertThat(afterShare.path("version").asLong()).isEqualTo(1);
    var changed = new HashMap<>(input);
    changed.put("durationMinutes", 45);
    assertThat(
            call("athlete", "POST", "/api/v1/me/training-sessions", changed)
                .body()
                .path("code")
                .asText())
        .isEqualTo("TRAINING_ID_CONFLICT");
    assertThat(
            db.sql("select count(*) from training_review where training_session_id=:id")
                .param("id", id)
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void concurrentRetriesCreateExactlyOneSession() throws Exception {
    var id = UUID.randomUUID();
    var input = training(id);
    var start = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> request =
          () -> {
            start.await(10, TimeUnit.SECONDS);
            return call("athlete", "POST", "/api/v1/me/training-sessions", input);
          };
      var a = executor.submit(request);
      var b = executor.submit(request);
      var first = a.get(20, TimeUnit.SECONDS);
      var second = b.get(20, TimeUnit.SECONDS);
      assertThat(first.status()).isEqualTo(201);
      assertThat(second.status()).isEqualTo(201);
      assertThat(first.body()).isEqualTo(second.body());
    }
    assertThat(
            db.sql("select count(*) from training_session where id=:id")
                .param("id", id)
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void anotherOwnerCannotReuseTheIdentifierOrReadItsContent() throws Exception {
    var id = UUID.randomUUID();
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(id), 201);
    var profile = UUID.randomUUID();
    db.sql(
            "insert into practitioner_profile(id,user_id,martial_art_id,status) values(:id,'20000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000001','ACTIVE')")
        .param("id", profile)
        .update();
    try {
      var input = new HashMap<>(training(id));
      input.put("practitionerProfileId", profile);
      input.put("organizationId", OTHER_DOJO);
      var result = call("outsider", "POST", "/api/v1/me/training-sessions", input);
      assertThat(result.status()).isEqualTo(409);
      assertThat(result.body().toString()).doesNotContain("sleeve grip");
      assertThat(call("outsider", "GET", "/api/v1/me/training-sessions/" + id, null).status())
          .isEqualTo(404);
    } finally {
      db.sql("delete from practitioner_profile where id=:id").param("id", profile).update();
    }
  }

  @Test
  void inactiveProfileKeepsHistoryButCannotShare() throws Exception {
    var id = UUID.randomUUID();
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(id), 201);
    db.sql("update practitioner_profile set status='INACTIVE' where id=:id")
        .param("id", PROFILE)
        .update();
    try {
      assertThat(
              call(
                      "athlete",
                      "POST",
                      "/api/v1/me/training-sessions/" + id + "/share",
                      Map.of("version", 0))
                  .status())
          .isEqualTo(404);
      assertThat(
              ok("athlete", "GET", "/api/v1/me/training-sessions/" + id, null, 200)
                  .path("status")
                  .asText())
          .isEqualTo("RECORDED");
      assertThat(
              db.sql("select count(*) from training_review where training_session_id=:id")
                  .param("id", id)
                  .query(Long.class)
                  .single())
          .isZero();
    } finally {
      db.sql("update practitioner_profile set status='ACTIVE' where id=:id")
          .param("id", PROFILE)
          .update();
    }
  }

  @Test
  void revokedMembershipCannotShare() throws Exception {
    var id = UUID.randomUUID();
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(id), 201);
    db.sql(
            "update organization_membership set active=false where organization_id=:org and user_id='20000000-0000-0000-0000-000000000001'")
        .param("org", DOJO)
        .update();
    try {
      assertThat(
              call(
                      "athlete",
                      "POST",
                      "/api/v1/me/training-sessions/" + id + "/share",
                      Map.of("version", 0))
                  .status())
          .isEqualTo(403);
    } finally {
      db.sql(
              "update organization_membership set active=true where organization_id=:org and user_id='20000000-0000-0000-0000-000000000001'")
          .param("org", DOJO)
          .update();
    }
  }
}
