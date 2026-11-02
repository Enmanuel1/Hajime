package com.hajime.practitioner.application;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreatePractitionerProfile(
    @NotBlank String martialArtCode,
    @Positive @Digits(integer = 3, fraction = 2) BigDecimal weightKg) {
  @JsonAnySetter
  public void unknown(String field, Object value) {
    throw new IllegalArgumentException(
        "Profile creation accepts no ownership, role or rank fields.");
  }
}
