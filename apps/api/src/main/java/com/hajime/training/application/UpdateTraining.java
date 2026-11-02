package com.hajime.training.application;

import static com.hajime.shared.DomainValidation.required;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.hajime.training.application.RecordTraining.Sparring;
import com.hajime.training.application.RecordTraining.Technique;
import com.hajime.training.domain.TrainingContent;
import com.hajime.training.domain.TrainingSession.PracticeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public record UpdateTraining(
    @NotNull @Min(0) Long version,
    UUID organizationId,
    @NotNull @PastOrPresent Instant occurredAt,
    @Min(1) @Max(720) int durationMinutes,
    @NotNull PracticeType practiceType,
    @Min(1) @Max(10) Integer perceivedIntensity,
    @Min(1) @Max(5) Integer energyLevel,
    @Size(max = 4000) String reflection,
    @NotNull @Size(max = 100) List<@NotNull @Valid Technique> techniques,
    @NotNull @Size(max = 100) List<@NotNull @Valid Sparring> sparring) {
  public TrainingContent toContent() {
    return new TrainingContent(
        occurredAt,
        durationMinutes,
        practiceType,
        perceivedIntensity,
        energyLevel,
        reflection,
        required(techniques, "Techniques").stream().map(RecordTraining.Technique::toEntry).toList(),
        required(sparring, "Sparring").stream().map(RecordTraining.Sparring::toEntry).toList());
  }

  @JsonAnySetter
  public void unknown(String field, Object value) {
    throw new IllegalArgumentException("Unknown training update field.");
  }
}
