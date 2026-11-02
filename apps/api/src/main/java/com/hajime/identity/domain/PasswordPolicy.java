package com.hajime.identity.domain;

import com.hajime.shared.DomainValidation;

public final class PasswordPolicy {
  private PasswordPolicy() {}

  public static void require(String password) {
    DomainValidation.require(
        password != null
            && password.length() >= 15
            && password.length() <= 128
            && !password.isBlank(),
        "Password must contain 15..128 characters.");
  }
}
