package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.ObjectMapper;

class ProductionJwtSecurityTest extends ProductionPostgresTest {
  @LocalServerPort int port;
  @Autowired ObjectMapper json;

  String token(
      String issuer,
      String audience,
      String type,
      String subject,
      String sid,
      Instant expiry,
      byte[] key) {
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(subject)
            .issuedAt(Instant.now().minusSeconds(600))
            .claim("sid", sid)
            .claim("token_use", type);
    if (expiry != null) claims.expiresAt(expiry);
    var encoder =
        NimbusJwtEncoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
            .algorithm(MacAlgorithm.HS256)
            .build();
    return encoder
        .encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
        .getTokenValue();
  }

  HttpResponse<String> request(String token) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/me"))
                .header("Authorization", "Bearer " + token)
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void ownSignedTokensAuthenticateAndOtherClaimsAreRejected() throws Exception {
    var body =
        Map.of(
            "email",
            "jwt-" + UUID.randomUUID() + "@example.test",
            "password",
            "A long private test passphrase",
            "displayName",
            "JWT athlete");
    var registered =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(registered.statusCode()).describedAs(registered.body()).isEqualTo(201);
    var response = json.readTree(registered.body());
    var access = response.path("accessToken").asText();
    assertThat(request(access).statusCode()).isEqualTo(200);
    var payload =
        json.readTree(
            new String(
                Base64.getUrlDecoder().decode(access.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8));
    var subject = payload.path("sub").asText();
    var sid = payload.path("sid").asText();
    var key = Base64.getDecoder().decode(JWT_SECRET);
    var expiry = Instant.now().plusSeconds(300);
    byte[] other = new byte[32];
    new java.security.SecureRandom().nextBytes(other);
    var invalid =
        List.of(
            token("wrong", "hajime-app", "access", subject, sid, expiry, key),
            token("hajime", "wrong", "access", subject, sid, expiry, key),
            token("hajime", "hajime-app", "refresh", subject, sid, expiry, key),
            token(
                "hajime",
                "hajime-app",
                "access",
                subject,
                sid,
                Instant.now().minusSeconds(300),
                key),
            token("hajime", "hajime-app", "access", subject, sid, expiry, other),
            token("hajime", "hajime-app", "access", subject, sid, null, key),
            token(
                "hajime",
                "hajime-app",
                "access",
                subject,
                UUID.randomUUID().toString(),
                expiry,
                key),
            token(
                "hajime", "hajime-app", "access", "local:" + UUID.randomUUID(), sid, expiry, key));
    for (var bad : invalid) {
      var result = request(bad);
      assertThat(result.statusCode()).isEqualTo(401);
      assertThat(result.body()).contains("UNAUTHENTICATED");
    }
  }
}
