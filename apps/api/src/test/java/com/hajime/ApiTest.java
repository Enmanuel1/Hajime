package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.*;

@ActiveProfiles("test")
abstract class ApiTest extends PostgresTest {
  static final UUID DOJO = UUID.fromString("30000000-0000-0000-0000-000000000001");
  static final UUID OTHER_DOJO = UUID.fromString("30000000-0000-0000-0000-000000000002");
  static final UUID PROFILE = UUID.fromString("40000000-0000-0000-0000-000000000001");
  static final UUID TECHNIQUE = UUID.fromString("14000000-0000-0000-0000-000000000008");
  @LocalServerPort int port;
  @Autowired ObjectMapper json;
  private final HttpClient client = HttpClient.newHttpClient();

  record Result(int status, JsonNode body) {}

  Result call(String user, String method, String path, Object body) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("X-Dev-User", user)
            .header("Content-Type", "application/json");
    builder.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return new Result(
        response.statusCode(),
        response.body().isEmpty() ? json.nullNode() : json.readTree(response.body()));
  }

  JsonNode ok(String user, String method, String path, Object body, int status) throws Exception {
    var result = call(user, method, path, body);
    assertThat(result.status()).describedAs(result.body().toString()).isEqualTo(status);
    return result.body();
  }

  Map<String, Object> training(UUID id) {
    return Map.of(
        "id",
        id,
        "practitionerProfileId",
        PROFILE,
        "organizationId",
        DOJO,
        "occurredAt",
        "2026-10-31T22:00:00Z",
        "durationMinutes",
        90,
        "practiceType",
        "MIXED",
        "reflection",
        "Kept my sleeve grip through the entry. Need more control in the reap.",
        "techniques",
        List.of(Map.of("techniqueId", TECHNIQUE, "repetitions", 20)),
        "sparring",
        List.of(Map.of("rounds", 4, "intensity", 6)));
  }
}
