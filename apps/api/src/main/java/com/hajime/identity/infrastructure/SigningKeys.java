package com.hajime.identity.infrastructure;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

final class SigningKeys {
  private SigningKeys() {}

  static SecretKey resolve(String encoded, boolean development) {
    byte[] key;
    if (encoded == null || encoded.isBlank()) {
      if (!development)
        throw new IllegalStateException(
            "Set HAJIME_JWT_SECRET to a Base64-encoded random key of at least 32 bytes.");
      key = new byte[32];
      new SecureRandom().nextBytes(key);
    } else {
      try {
        key = Base64.getDecoder().decode(encoded);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("HAJIME_JWT_SECRET must be valid Base64.");
      }
      if (key.length < 32)
        throw new IllegalStateException("HAJIME_JWT_SECRET must decode to at least 32 bytes.");
    }
    return new SecretKeySpec(key, "HmacSHA256");
  }
}
