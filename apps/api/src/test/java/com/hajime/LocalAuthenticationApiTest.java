package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.*;

/** No dev/test profile: every protected request uses the real local authentication chain. */
class LocalAuthenticationApiTest extends ProductionPostgresTest {
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

  @Test
  void realAccountCreatesOwnProfileAndPersonalTraining() throws Exception {
    var auth = register(email());
    var token = access(auth);
    assertThat(request("GET", "/api/v1/me", null, token).body().path("id").asText())
        .isEqualTo(auth.path("user").path("id").asText());
    assertThat(request("GET", "/api/v1/me/practitioner-profiles", null, token).body()).isEmpty();
    var created =
        request(
            "POST",
            "/api/v1/me/practitioner-profiles",
            Map.of("martialArtCode", "JUDO", "weightKg", 80.5),
            token);
    assertThat(created.status()).isEqualTo(201);
    var profile = created.body();
    assertThat(profile.path("userId").asText()).isEqualTo(auth.path("user").path("id").asText());
    assertThat(profile.path("currentRankId").isNull()).isTrue();
    assertThat(profile.path("status").asText()).isEqualTo("ACTIVE");
    assertThat(
            request("GET", created.headers().firstValue("Location").orElseThrow(), null, token)
                .status())
        .isEqualTo(200);
    var training =
        request(
            "POST",
            "/api/v1/me/training-sessions",
            Map.of(
                "practitionerProfileId",
                profile.path("id").asText(),
                "occurredAt",
                "2026-10-31T12:00:00Z",
                "durationMinutes",
                60,
                "practiceType",
                "TECHNICAL",
                "techniques",
                List.of(),
                "sparring",
                List.of()),
            token);
    assertThat(training.status()).isEqualTo(201);
    assertThat(training.body().path("source").asText()).isEqualTo("PERSONAL");
    var another = access(register(email()));
    assertThat(
            request(
                    "GET",
                    "/api/v1/me/practitioner-profiles/" + profile.path("id").asText(),
                    null,
                    another)
                .status())
        .isEqualTo(404);
    assertThat(
            request(
                    "GET",
                    "/api/v1/me/training-sessions/" + training.body().path("id").asText(),
                    null,
                    another)
                .status())
        .isEqualTo(404);
    assertThat(
            request(
                    "POST",
                    "/api/v1/organizations/30000000-0000-0000-0000-000000000001/practitioners/"
                        + profile.path("id").asText()
                        + "/goals",
                    Map.of("title", "Fake coach", "objective", "Should fail", "priority", "NORMAL"),
                    token)
                .status())
        .isEqualTo(403);
  }

  @Test
  void emailNormalizationAndCredentialsAreProtected() throws Exception {
    var email = email();
    var auth = register(email.toUpperCase(Locale.ROOT));
    var login =
        request(
            "POST",
            "/api/v1/auth/login",
            Map.of("email", " " + email + " ", "password", PASSWORD),
            null);
    assertThat(login.status()).isEqualTo(200);
    assertThat(login.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/register",
                    Map.of("email", email, "password", PASSWORD, "displayName", "Duplicate"),
                    null)
                .status())
        .isEqualTo(409);
    for (var e : List.of(email, email())) {
      var bad =
          request(
              "POST",
              "/api/v1/auth/login",
              Map.of("email", e, "password", "Wrong long private passphrase"),
              null);
      assertThat(bad.status()).isEqualTo(401);
      assertThat(bad.body().path("code").asText()).isEqualTo("INVALID_CREDENTIALS");
    }
    var hash =
        db.sql("select password_hash from local_credential where email=:email")
            .param("email", email)
            .query(String.class)
            .single();
    assertThat(hash).startsWith("{pbkdf2-sha256-v1}").doesNotContain(PASSWORD);
    assertThat(
            db.sql("select count(*) from refresh_token where token_hash=:raw")
                .param("raw", refresh(auth))
                .query(Long.class)
                .single())
        .isZero();
  }

  @Test
  void refreshRotatesAndReplayRevokesOnlyItsOwnSession() throws Exception {
    var email = email();
    var auth = register(email);
    var another =
        request("POST", "/api/v1/auth/login", Map.of("email", email, "password", PASSWORD), null)
            .body();
    var rotated =
        request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(auth)), null);
    assertThat(rotated.status()).isEqualTo(200);
    assertThat(refresh(rotated.body())).isNotEqualTo(refresh(auth));
    assertThat(request("GET", "/api/v1/me", null, access(rotated.body())).status()).isEqualTo(200);
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(auth)), null)
                .status())
        .isEqualTo(401);
    assertThat(request("GET", "/api/v1/me", null, access(rotated.body())).status()).isEqualTo(401);
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/refresh",
                    Map.of("refreshToken", refresh(rotated.body())),
                    null)
                .status())
        .isEqualTo(401);
    assertThat(request("GET", "/api/v1/me", null, access(another)).status()).isEqualTo(200);
  }

  @Test
  void logoutImmediatelyRevokesAccessAndRefreshAndIsIdempotent() throws Exception {
    var auth = register(email());
    for (int i = 0; i < 2; i++)
      assertThat(
              request("POST", "/api/v1/auth/logout", Map.of("refreshToken", refresh(auth)), null)
                  .status())
          .isEqualTo(204);
    assertThat(request("GET", "/api/v1/me", null, access(auth)).status()).isEqualTo(401);
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(auth)), null)
                .status())
        .isEqualTo(401);
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/logout",
                    Map.of(
                        "refreshToken",
                        com.hajime.identity.infrastructure.RefreshTokens.generate()),
                    null)
                .status())
        .isEqualTo(204);
  }

  @Test
  void parallelRefreshCannotCreateTwoUsableBranches() throws Exception {
    var auth = register(email());
    var start = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> rotate =
          () -> {
            start.await(10, TimeUnit.SECONDS);
            return request(
                "POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(auth)), null);
          };
      var a = executor.submit(rotate);
      var b = executor.submit(rotate);
      var first = a.get(20, TimeUnit.SECONDS);
      var second = b.get(20, TimeUnit.SECONDS);
      assertThat(List.of(first.status(), second.status())).containsExactlyInAnyOrder(200, 401);
      var issued = first.status() == 200 ? first.body() : second.body();
      assertThat(request("GET", "/api/v1/me", null, access(issued)).status()).isEqualTo(401);
    }
  }

  @Test
  void duplicateRegistrationRollsBackTheExtraIdentity() throws Exception {
    var email = email();
    var start = new CyclicBarrier(2);
    var before = db.sql("select count(*) from app_user").query(Long.class).single();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> register =
          () -> {
            start.await(10, TimeUnit.SECONDS);
            return request(
                "POST",
                "/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "displayName", "Race"),
                null);
          };
      var a = executor.submit(register);
      var b = executor.submit(register);
      assertThat(
              List.of(a.get(20, TimeUnit.SECONDS).status(), b.get(20, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(db.sql("select count(*) from app_user").query(Long.class).single())
        .isEqualTo(before + 1);
  }

  @Test
  void revokedAccountOrExpiredSessionCannotAuthenticate() throws Exception {
    var email = email();
    var auth = register(email);
    db.sql("update local_credential set active=false where email=:email")
        .param("email", email)
        .update();
    assertThat(request("GET", "/api/v1/me", null, access(auth)).status()).isEqualTo(401);
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/login",
                    Map.of("email", email, "password", PASSWORD),
                    null)
                .status())
        .isEqualTo(401);
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(auth)), null)
                .status())
        .isEqualTo(401);
    var expired = register(email());
    db.sql(
            "update auth_session set created_at=now()-interval '2 days',expires_at=now()-interval '1 day' where id=:id")
        .param("id", session(expired))
        .update();
    assertThat(request("GET", "/api/v1/me", null, access(expired)).status()).isEqualTo(401);
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(expired)), null)
                .status())
        .isEqualTo(401);
  }

  @Test
  void profileCreationIsUniqueAndCannotChooseOwnershipOrGrade() throws Exception {
    var auth = register(email());
    var token = access(auth);
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/practitioner-profiles",
                    Map.of("martialArtCode", "JUDO", "userId", UUID.randomUUID()),
                    token)
                .status())
        .isEqualTo(400);
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/practitioner-profiles",
                    Map.of("martialArtCode", "JUDO", "currentRankId", UUID.randomUUID()),
                    token)
                .status())
        .isEqualTo(400);
    var gate = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> create =
          () -> {
            gate.await(10, TimeUnit.SECONDS);
            return request(
                "POST",
                "/api/v1/me/practitioner-profiles",
                Map.of("martialArtCode", "JUDO"),
                token);
          };
      var a = executor.submit(create);
      var b = executor.submit(create);
      assertThat(
              List.of(a.get(10, TimeUnit.SECONDS).status(), b.get(10, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(201, 409);
    }
    var invalid =
        request(
            "POST",
            "/api/v1/me/practitioner-profiles",
            Map.of("martialArtCode", "JUDO", "weightKg", -1),
            token);
    assertThat(invalid.status()).isEqualTo(400);
  }

  @Test
  void invalidInputsAndPrivilegeFieldsAreRejectedBeforeRegistration() throws Exception {
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/register",
                    Map.of("email", email(), "password", "short", "displayName", "New user"),
                    null)
                .status())
        .isEqualTo(400);
    assertThat(
            request(
                    "POST",
                    "/api/v1/auth/register",
                    Map.of(
                        "email",
                        email(),
                        "password",
                        PASSWORD,
                        "displayName",
                        "New user",
                        "role",
                        "ADMIN"),
                    null)
                .status())
        .isEqualTo(400);
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", "invalid"), null)
                .status())
        .isEqualTo(401);
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/practitioner-profiles",
                    Map.of("martialArtCode", "JUDO"),
                    null)
                .status())
        .isEqualTo(401);
  }

  @Autowired com.hajime.identity.infrastructure.AuthRepository accounts;

  @Test
  void expiryCleanupRetainsActiveFamiliesAndRemovesExpiredRefreshHistory() throws Exception {
    var active = register(email());
    var expired = register(email());
    var id = session(expired);
    db.sql(
            "update auth_session set created_at=now()-interval '3 days',expires_at=now()-interval '2 days' where id=:id")
        .param("id", id)
        .update();
    assertThat(accounts.purgeExpiredSessions()).isBetween(1, 1000);
    assertThat(
            db.sql("select count(*) from refresh_token where session_id=:id")
                .param("id", id)
                .query(Long.class)
                .single())
        .isZero();
    assertThat(
            request("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(expired)), null)
                .status())
        .isEqualTo(401);
    assertThat(request("GET", "/api/v1/me", null, access(active)).status()).isEqualTo(200);
  }
}
