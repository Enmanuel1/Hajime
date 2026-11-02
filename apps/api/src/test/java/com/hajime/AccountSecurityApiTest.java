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
class AccountSecurityApiTest extends ProductionApiSupport {
  static final String NEW_PASSWORD = "A different long private passphrase";

  Result login(String email, String password) throws Exception {
    return request(
        "POST", "/api/v1/auth/login", Map.of("email", email, "password", password), null);
  }

  Result generate(JsonNode account) throws Exception {
    return request(
        "POST", "/api/v1/me/recovery-code", Map.of("currentPassword", PASSWORD), access(account));
  }

  Result recover(String email, String code) throws Exception {
    return request(
        "POST",
        "/api/v1/auth/recover",
        Map.of("email", email, "recoveryCode", code, "newPassword", NEW_PASSWORD),
        null);
  }

  @Test
  void passwordChangeRequiresProofAndRevokesEverySessionAndRecoveryCode() throws Exception {
    var email = email();
    var a = register(email);
    var b = login(email, PASSWORD).body();
    var code = generate(a).body().path("recoveryCode").asText();
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/password",
                    Map.of(
                        "currentPassword",
                        "Incorrect private password",
                        "newPassword",
                        NEW_PASSWORD),
                    access(a))
                .status())
        .isEqualTo(401);
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/password",
                    Map.of("currentPassword", PASSWORD, "newPassword", "short"),
                    access(a))
                .status())
        .isEqualTo(400);
    var result =
        request(
            "POST",
            "/api/v1/me/password",
            Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD),
            access(a));
    assertThat(result.status()).isEqualTo(204);
    assertThat(result.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    for (var session : List.of(a, b)) {
      assertThat(request("GET", "/api/v1/me", null, access(session)).status()).isEqualTo(401);
      assertThat(
              request(
                      "POST",
                      "/api/v1/auth/refresh",
                      Map.of("refreshToken", refresh(session)),
                      null)
                  .status())
          .isEqualTo(401);
    }
    assertThat(login(email, PASSWORD).status()).isEqualTo(401);
    assertThat(login(email, NEW_PASSWORD).status()).isEqualTo(200);
    assertThat(recover(email, code).status()).isEqualTo(401);
  }

  @Test
  void recoveryCodeIsHashedRotatedSingleUseAndCannotResetAnotherAccount() throws Exception {
    var email = email();
    var a = register(email);
    var anotherEmail = email();
    var another = register(anotherEmail);
    var first = generate(a);
    assertThat(first.status()).isEqualTo(200);
    assertThat(first.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    var old = first.body().path("recoveryCode").asText();
    var current = generate(a).body().path("recoveryCode").asText();
    assertThat(current).hasSize(43).isNotEqualTo(old);
    var hash =
        db.sql("select recovery_code_hash from local_credential where email=:email")
            .param("email", email)
            .query(String.class)
            .single();
    assertThat(hash)
        .isEqualTo(com.hajime.identity.infrastructure.RefreshTokens.hash(current))
        .doesNotContain(current);
    assertThat(recover(email, old).status()).isEqualTo(401);
    assertThat(recover(anotherEmail, current).status()).isEqualTo(401);
    assertThat(recover(email(), current).status()).isEqualTo(401);
    assertThat(request("GET", "/api/v1/me", null, access(a)).status()).isEqualTo(200);
    assertThat(recover(email, current).status()).isEqualTo(204);
    assertThat(recover(email, current).status()).isEqualTo(401);
    assertThat(request("GET", "/api/v1/me", null, access(a)).status()).isEqualTo(401);
    assertThat(login(email, NEW_PASSWORD).status()).isEqualTo(200);
    assertThat(request("GET", "/api/v1/me", null, access(another)).status()).isEqualTo(200);
    assertThat(
            request(
                    "POST",
                    "/api/v1/me/recovery-code",
                    Map.of("currentPassword", NEW_PASSWORD),
                    null)
                .status())
        .isEqualTo(401);
  }

  @Test
  void competingRecoveriesCannotBothConsumeTheSameCode() throws Exception {
    var email = email();
    var account = register(email);
    var code = generate(account).body().path("recoveryCode").asText();
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> action =
          () -> {
            gate.await(10, TimeUnit.SECONDS);
            return recover(email, code);
          };
      var a = pool.submit(action);
      var b = pool.submit(action);
      assertThat(
              List.of(a.get(20, TimeUnit.SECONDS).status(), b.get(20, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(204, 401);
    }
    assertThat(
            db.sql(
                    "select count(*) from audit_log where actor_user_id=:user and action='ACCOUNT_RECOVERED'")
                .param("user", UUID.fromString(account.path("user").path("id").asText()))
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void refreshAndPasswordChangeDoNotLeaveAnyOldSessionUsable() throws Exception {
    var email = email();
    var a = register(email);
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var change =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request(
                    "POST",
                    "/api/v1/me/password",
                    Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD),
                    access(a));
              });
      var rotate =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request(
                    "POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh(a)), null);
              });
      assertThat(change.get(20, TimeUnit.SECONDS).status()).isEqualTo(204);
      var rotation = rotate.get(20, TimeUnit.SECONDS);
      assertThat(rotation.status()).isIn(200, 401);
      if (rotation.status() == 200)
        assertThat(request("GET", "/api/v1/me", null, access(rotation.body())).status())
            .isEqualTo(401);
    }
    assertThat(login(email, NEW_PASSWORD).status()).isEqualTo(200);
  }
}
