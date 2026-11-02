package com.hajime.identity.infrastructure;

import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens {
  private final JwtEncoder encoder;
  private final AuthSettings settings;

  public JwtTokens(@Qualifier("localJwtKey") SecretKey key, AuthSettings settings) {
    this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    this.settings = settings;
  }

  public String issue(UUID user, UUID session, Instant now, Instant expires) {
    var claims =
        JwtClaimsSet.builder()
            .issuer(settings.issuer())
            .audience(List.of(settings.audience()))
            .subject("local:" + user)
            .issuedAt(now)
            .expiresAt(expires)
            .id(UUID.randomUUID().toString())
            .claim("sid", session.toString())
            .claim("token_use", "access")
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
