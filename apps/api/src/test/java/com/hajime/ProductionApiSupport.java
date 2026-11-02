package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.*;

/** Reusable real HTTP/JWT fixtures without development aliases. */
abstract class ProductionApiSupport extends ProductionPostgresTest {
  @LocalServerPort int port;
  @Autowired ObjectMapper json;
  @Autowired JdbcClient db;
  final HttpClient client = HttpClient.newHttpClient();
  static final String PASSWORD = "My private long test passphrase";

  record Result(int status, JsonNode body, HttpHeaders headers) {}

  Result request(String method, String path, Object body, String token) throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json");
    if (token != null) b.header("Authorization", "Bearer " + token);
    var response =
        client.send(
            b.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return new Result(
        response.statusCode(),
        response.body().isEmpty() ? json.nullNode() : json.readTree(response.body()),
        response.headers());
  }

  String email() {
    return "athlete-" + UUID.randomUUID() + "@example.test";
  }

  JsonNode register(String email) throws Exception {
    var result =
        request(
            "POST",
            "/api/v1/auth/register",
            Map.of("email", email, "password", PASSWORD, "displayName", "New athlete"),
            null);
    assertThat(result.status()).isEqualTo(201);
    return result.body();
  }

  String access(JsonNode auth) {
    return auth.path("accessToken").asText();
  }

  String refresh(JsonNode auth) {
    return auth.path("refreshToken").asText();
  }

  UUID session(JsonNode auth) throws Exception {
    var payload =
        json.readTree(
            new String(
                Base64.getUrlDecoder().decode(access(auth).split("[.]")[1]),
                java.nio.charset.StandardCharsets.UTF_8));
    return UUID.fromString(payload.path("sid").asText());
  }
}
