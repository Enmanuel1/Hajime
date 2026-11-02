package com.hajime.training.application;

import static com.hajime.shared.DomainValidation.required;

import com.hajime.training.domain.TrainingContent;
import com.hajime.training.domain.TrainingSession.PracticeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public record RecordTraining(
    UUID id,
    @NotNull UUID practitionerProfileId,
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
        required(techniques, "Techniques").stream()
            .map(t -> required(t, "Technique entry").toEntry())
            .toList(),
        required(sparring, "Sparring").stream()
            .map(s -> required(s, "Sparring entry").toEntry())
            .toList());
  }

  public record Technique(
      @NotNull UUID techniqueId,
      @Min(1) Integer repetitions,
      @Min(1) @Max(720) Integer durationMinutes,
      @Size(max = 1000) String notes) {
    TrainingContent.TechniqueEntry toEntry() {
      return new TrainingContent.TechniqueEntry(techniqueId, repetitions, durationMinutes, notes);
    }
  }

  public record Sparring(
      @Min(1) @Max(100) Integer rounds,
      @Min(1) @Max(10) Integer intensity,
      @Size(max = 1000) String notes) {
    TrainingContent.SparringEntry toEntry() {
      return new TrainingContent.SparringEntry(rounds, intensity, notes);
    }
  }
}
