package com.hajime.martialart;

import java.util.Locale;

public record MartialArtCode(String value) {
  public MartialArtCode {
    if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]{1,29}"))
      throw com.hajime.shared.BusinessException.invalid(
          "MARTIAL_ART_NOT_SUPPORTED", "Invalid martial art code.");
    value = value.toUpperCase(Locale.ROOT);
  }
}
