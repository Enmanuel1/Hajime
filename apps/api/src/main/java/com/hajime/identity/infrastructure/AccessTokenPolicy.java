package com.hajime.identity.infrastructure;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

/** Cognito access tokens identify their app client using client_id, not ID-token aud. */
public final class AccessTokenPolicy implements OAuth2TokenValidator<Jwt> {
  private final String clientId;

  public AccessTokenPolicy(String clientId) {
    if (clientId == null || clientId.isBlank())
      throw new IllegalStateException(
          "Configure hajime.security.jwt.client-id before enabling JWT authentication.");
    this.clientId = clientId;
  }

  public OAuth2TokenValidatorResult validate(Jwt token) {
    if ("access".equals(token.getClaims().get("token_use"))
        && clientId.equals(token.getClaims().get("client_id"))
        && token.getSubject() != null
        && !token.getSubject().isBlank()
        && token.getExpiresAt() != null) return OAuth2TokenValidatorResult.success();
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Invalid access token claims.", null));
  }
}
