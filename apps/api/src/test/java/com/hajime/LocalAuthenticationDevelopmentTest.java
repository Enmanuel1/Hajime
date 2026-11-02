package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocalAuthenticationDevelopmentTest extends ApiTest {
  @Test
  void explicitBearerCannotFallBackToADevelopmentIdentity() throws Exception {
    var registered =
        ok(
            "sensei",
            "POST",
            "/api/v1/auth/register",
            Map.of(
                "email",
                "devjwt-" + UUID.randomUUID() + "@example.test",
                "password",
                "Long private development passphrase",
                "displayName",
                "Real account"),
            201);
    var client = HttpClient.newHttpClient();
    var url = URI.create("http://127.0.0.1:" + port + "/api/v1/me");
    var valid =
        client.send(
            HttpRequest.newBuilder(url)
                .header("Authorization", "Bearer " + registered.path("accessToken").asText())
                .header("X-Dev-User", "sensei")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(valid.statusCode()).isEqualTo(200);
    assertThat(json.readTree(valid.body()).path("id").asText())
        .isEqualTo(registered.path("user").path("id").asText());
    var invalid =
        client.send(
            HttpRequest.newBuilder(url)
                .header("Authorization", "Bearer invalid")
                .header("X-Dev-User", "athlete")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(invalid.statusCode()).isEqualTo(401);
  }
}
