package com.hajime;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "hajime.security.jwt.client-id=hajime-client")
@Import(ProductionJwtSecurityTest.Keys.class)
class ProductionJwtSecurityTest extends ProductionPostgresTest {
  static final String ISSUER = "https://issuer.example.test/pool";
  static final RSAKey KEY = key();

  static RSAKey key() {
    try {
      return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Keys {
    @Bean
    JwtDecoder jwtDecoder() throws Exception {
      var decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
      decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
      return decoder;
    }
  }

  @LocalServerPort int port;
  @Autowired JdbcClient db;

  @BeforeEach
  void provision() {
    db.sql(
            "insert into app_user(id,subject,display_name) values('29000000-0000-0000-0000-000000000001','jwt-athlete','JWT athlete') on conflict do nothing")
        .update();
  }

  String token(
      String client, String type, String issuer, String subject, Instant expiry, RSAKey key) {
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(subject)
            .issuedAt(Instant.now().minusSeconds(600))
            .claim("client_id", client)
            .claim("token_use", type);
    if (expiry != null) claims.expiresAt(expiry);
    var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(),
                claims.build()))
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
  void signedAccessTokenAuthenticatesAProvisionedUser() throws Exception {
    var response =
        request(
            token(
                "hajime-client",
                "access",
                ISSUER,
                "jwt-athlete",
                Instant.now().plusSeconds(300),
                KEY));
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("JWT athlete");
  }

  @Test
  void clientTypeIssuerExpiryAndSignatureAreChecked() throws Exception {
    var expiry = Instant.now().plusSeconds(300);
    var invalid =
        List.of(
            token("other-client", "access", ISSUER, "jwt-athlete", expiry, KEY),
            token("hajime-client", "id", ISSUER, "jwt-athlete", expiry, KEY),
            token(
                "hajime-client",
                "access",
                "https://other.example.test",
                "jwt-athlete",
                expiry,
                KEY),
            token(
                "hajime-client",
                "access",
                ISSUER,
                "jwt-athlete",
                Instant.now().minusSeconds(300),
                KEY),
            token("hajime-client", "access", ISSUER, "jwt-athlete", expiry, key()),
            token("hajime-client", "access", ISSUER, " ", expiry, KEY),
            token("hajime-client", "access", ISSUER, "jwt-athlete", null, KEY));
    for (var token : invalid) {
      var response = request(token);
      assertThat(response.statusCode()).isEqualTo(401);
      assertThat(response.body()).contains("UNAUTHENTICATED");
    }
  }

  @Test
  void validButUnprovisionedIdentityIsRejected() throws Exception {
    assertThat(
            request(
                    token(
                        "hajime-client",
                        "access",
                        ISSUER,
                        "unknown-user",
                        Instant.now().plusSeconds(300),
                        KEY))
                .statusCode())
        .isEqualTo(401);
  }
}
