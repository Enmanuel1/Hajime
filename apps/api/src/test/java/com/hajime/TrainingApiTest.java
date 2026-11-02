package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class TrainingApiTest extends ApiTest {
  @Test
  void recordFetchPaginateAndShare() throws Exception {
    var id = UUID.randomUUID();
    String path = "/api/v1/me/training-sessions/" + id;
    var created = ok("athlete", "POST", "/api/v1/me/training-sessions", training(id), 201);
    assertThat(created.path("source").asText()).isEqualTo("PERSONAL");
    assertThat(created.path("version").asLong()).isZero();
    assertThat(ok("athlete", "GET", path, null, 200).path("techniques").size()).isEqualTo(1);
    assertThat(
            ok("athlete", "GET", "/api/v1/me/training-sessions?size=1", null, 200)
                .path("content")
                .size())
        .isEqualTo(1);
    assertThat(call("outsider", "GET", path, null).status()).isEqualTo(404);
    assertThat(call("sensei", "POST", path + "/share", Map.of("version", 0)).status())
        .isEqualTo(404);
    var shared = ok("athlete", "POST", path + "/share", Map.of("version", 0), 200);
    assertThat(shared.path("status").asText()).isEqualTo("SHARED_WITH_COACH");
    assertThat(call("athlete", "POST", path + "/share", Map.of("version", 0)).status())
        .isEqualTo(409);
  }

  @Test
  void invalidTrainingAndOrganizationAreRejected() throws Exception {
    var input = new HashMap<>(training(UUID.randomUUID()));
    input.put("durationMinutes", 0);
    assertThat(
            ok("athlete", "POST", "/api/v1/me/training-sessions", input, 400)
                .path("errors")
                .isArray())
        .isTrue();
    input.put("durationMinutes", 60);
    input.put("organizationId", OTHER_DOJO);
    assertThat(call("athlete", "POST", "/api/v1/me/training-sessions", input).status())
        .isEqualTo(403);
    assertThat(call("athlete", "GET", "/api/v1/me/training-sessions?size=1000", null).status())
        .isEqualTo(400);
  }
}
