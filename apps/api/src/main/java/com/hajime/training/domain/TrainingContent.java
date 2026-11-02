package com.hajime.training.domain;

import static com.hajime.shared.DomainValidation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Recorded observations; technical entry order is not part of their identity. */
public record TrainingContent(
    Instant occurredAt,
    int durationMinutes,
    TrainingSession.PracticeType practiceType,
    Integer perceivedIntensity,
    Integer energyLevel,
    String reflection,
    List<TechniqueEntry> techniques,
    List<SparringEntry> sparring) {
  public TrainingContent {
    occurredAt = required(occurredAt, "Occurrence time").truncatedTo(ChronoUnit.MICROS);
    required(practiceType, "Practice type");
    range(durationMinutes, 1, 720, "Duration");
    range(perceivedIntensity, 1, 10, "Intensity");
    range(energyLevel, 1, 5, "Energy");
    optionalText(reflection, 4000, "Reflection");
    require(
        techniques != null
            && techniques.size() <= 100
            && techniques.stream().noneMatch(Objects::isNull),
        "Invalid technique entries.");
    require(
        sparring != null && sparring.size() <= 100 && sparring.stream().noneMatch(Objects::isNull),
        "Invalid sparring entries.");
    techniques = List.copyOf(techniques);
    sparring = List.copyOf(sparring);
    if (techniques.stream().map(TechniqueEntry::techniqueId).distinct().count()
        != techniques.size())
      throw com.hajime.shared.BusinessException.invalid(
          "DUPLICATE_TECHNIQUE", "Each technique may occur once per session.");
  }

  public boolean sameRecording(TrainingContent other) {
    return occurredAt.equals(other.occurredAt)
        && durationMinutes == other.durationMinutes
        && practiceType == other.practiceType
        && Objects.equals(perceivedIntensity, other.perceivedIntensity)
        && Objects.equals(energyLevel, other.energyLevel)
        && Objects.equals(reflection, other.reflection)
        && new HashSet<>(techniques).equals(new HashSet<>(other.techniques))
        && sparring.equals(other.sparring);
  }

  public record TechniqueEntry(
      UUID techniqueId, Integer repetitions, Integer durationMinutes, String notes) {
    public TechniqueEntry {
      required(techniqueId, "Technique");
      range(repetitions, 1, Integer.MAX_VALUE, "Repetitions");
      range(durationMinutes, 1, 720, "Technique duration");
      optionalText(notes, 1000, "Technique notes");
    }
  }

  public record SparringEntry(Integer rounds, Integer intensity, String notes) {
    public SparringEntry {
      range(rounds, 1, 100, "Rounds");
      range(intensity, 1, 10, "Sparring intensity");
      optionalText(notes, 1000, "Sparring notes");
    }
  }
}
