package com.hajime.identity.infrastructure;

import java.time.Clock;
import java.util.UUID;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Signature/time/issuer validation runs first. This policy scopes tokens and enforces revocation.
 */
final class SessionTokenValidator implements OAuth2TokenValidator<Jwt> {
  private final AuthSettings settings;
  private final AuthRepository accounts;
  private final Clock clock;

  SessionTokenValidator(AuthSettings settings, AuthRepository accounts, Clock clock) {
    this.settings = settings;
    this.accounts = accounts;
    this.clock = clock;
  }

  public OAuth2TokenValidatorResult validate(Jwt token) {
    try {
      if (!"access".equals(token.getClaims().get("token_use"))
          || token.getExpiresAt() == null
          || token.getIssuedAt() == null
          || !token.getAudience().contains(settings.audience())
          || token.getSubject() == null
          || !token.getSubject().startsWith("local:")) return invalid();
      var user = UUID.fromString(token.getSubject().substring(6));
      var session = UUID.fromString(token.getClaimAsString("sid"));
      return accounts.activeSession(session, user, clock.instant())
          ? OAuth2TokenValidatorResult.success()
          : invalid();
    } catch (IllegalArgumentException | NullPointerException e) {
      return invalid();
    }
  }

  private OAuth2TokenValidatorResult invalid() {
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Invalid or revoked session.", null));
  }
}
