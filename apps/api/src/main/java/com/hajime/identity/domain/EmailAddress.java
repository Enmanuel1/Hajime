package com.hajime.identity.domain;

import com.hajime.shared.DomainValidation;
import java.util.Locale;

public record EmailAddress(String value) {
  public EmailAddress {
    DomainValidation.required(value, "Email");
    value = value.trim().toLowerCase(Locale.ROOT);
    DomainValidation.require(
        value.length() <= 254 && value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),
        "A valid email is required.");
  }
}
