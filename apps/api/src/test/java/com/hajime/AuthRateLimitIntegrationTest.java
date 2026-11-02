package com.hajime;

import static org.assertj.core.api.Assertions.*;

import com.hajime.identity.infrastructure.RefreshTokens;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"hajime.auth.max-ip-attempts=3", "hajime.auth.max-account-attempts=2"})
class AuthRateLimitIntegrationTest extends ProductionPostgresTest {
  @LocalServerPort int port;
  @Autowired JdbcClient db;
  @Autowired ObjectMapper json;

  @BeforeEach
  @AfterEach
  void clearIp() {
    db.sql("delete from auth_rate_limit where bucket_hash=:hash")
        .param("hash", RefreshTokens.hash("ip:127.0.0.1"))
        .update();
  }

  HttpResponse<String> login(String email) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(
                            Map.of("email", email, "password", "Wrong long test passphrase"))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void ipLimitAppliesBeforeExpensiveAuthentication() throws Exception {
    for (int i = 0; i < 3; i++)
      assertThat(login(UUID.randomUUID() + "@example.test").statusCode()).isEqualTo(401);
    var blocked = login(UUID.randomUUID() + "@example.test");
    assertThat(blocked.statusCode()).isEqualTo(429);
    assertThat(blocked.headers().firstValue("Retry-After")).isPresent();
  }

  @Test
  void accountLimitPersistsDespiteRejectedLoginTransactions() throws Exception {
    var email = UUID.randomUUID() + "@example.test";
    assertThat(login(email).statusCode()).isEqualTo(401);
    assertThat(login(email).statusCode()).isEqualTo(401);
    var blocked = login(email);
    assertThat(blocked.statusCode()).isEqualTo(429);
    assertThat(json.readTree(blocked.body()).path("code").asText()).isEqualTo("RATE_LIMITED");
  }

  @Test
  void recoveryAttemptsAreRateLimitedBeforeAValidCodeCanBeGuessed() throws Exception {
    var email = UUID.randomUUID() + "@example.test";
    for (int i = 0; i < 3; i++) {
      var response =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(
                          URI.create("http://127.0.0.1:" + port + "/api/v1/auth/recover"))
                      .header("Content-Type", "application/json")
                      .POST(
                          HttpRequest.BodyPublishers.ofString(
                              json.writeValueAsString(
                                  Map.of(
                                      "email",
                                      email,
                                      "recoveryCode",
                                      RefreshTokens.generate(),
                                      "newPassword",
                                      "A valid new private passphrase"))))
                      .build(),
                  HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(i < 2 ? 401 : 429);
    }
  }
}
