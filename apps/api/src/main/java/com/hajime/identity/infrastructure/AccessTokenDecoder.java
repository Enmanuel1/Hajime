package com.hajime.identity.infrastructure;

import org.springframework.security.oauth2.jwt.*;

/** Preserves the decoder's signature/time/issuer checks, then applies the app-client policy. */
final class AccessTokenDecoder implements JwtDecoder {
  private final JwtDecoder delegate;
  private final AccessTokenPolicy policy;

  AccessTokenDecoder(JwtDecoder delegate, String clientId) {
    this.delegate = delegate;
    this.policy = new AccessTokenPolicy(clientId);
  }

  public Jwt decode(String token) {
    var jwt = delegate.decode(token);
    var result = policy.validate(jwt);
    if (result.hasErrors())
      throw new JwtValidationException("Invalid access token.", result.getErrors());
    return jwt;
  }
}
