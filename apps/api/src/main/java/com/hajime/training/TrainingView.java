package com.hajime.training;

import com.hajime.shared.RecordSource;
import java.time.Instant;
import java.util.*;

public record TrainingView(
    UUID id,
    UUID practitionerProfileId,
    UUID martialArtId,
    UUID organizationId,
    Instant occurredAt,
    int durationMinutes,
    String practiceType,
    Integer perceivedIntensity,
    Integer energyLevel,
    String reflection,
    RecordSource source,
    String status,
    Instant createdAt,
    Instant updatedAt,
    Long version,
    List<TechniqueEntry> techniques,
    List<SparringEntry> sparring) {
  public record TechniqueEntry(
      UUID techniqueId, Integer repetitions, Integer durationMinutes, String notes) {}

  public record SparringEntry(Integer rounds, Integer intensity, String notes) {}
}
