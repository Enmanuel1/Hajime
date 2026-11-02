package com.hajime.coaching.domain;

import static com.hajime.shared.DomainValidation.*;

import com.hajime.shared.RecordSource;
import java.time.*;
import java.util.UUID;

public record Goal(
    UUID id,
    UUID practitionerProfileId,
    UUID organizationId,
    UUID martialArtId,
    UUID assignedByUserId,
    UUID sourceTrainingSessionId,
    UUID relatedTechniqueId,
    String title,
    String objective,
    String coachObservation,
    Priority priority,
    LocalDate reviewDate,
    Status status,
    RecordSource source,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public Goal {
    required(id, "Goal ID");
    required(practitionerProfileId, "Practitioner");
    required(organizationId, "Organization");
    required(martialArtId, "Martial art");
    required(assignedByUserId, "Author");
    required(priority, "Priority");
    required(status, "Status");
    required(source, "Source");
    required(createdAt, "Creation time");
    required(updatedAt, "Update time");
    require(version >= 0, "Invalid version.");
    title = text(title, 160, "Title");
    objective = text(objective, 2000, "Objective");
    optionalText(coachObservation, 2000, "Observation");
  }

  public static Goal assign(
      UUID profile,
      UUID org,
      UUID art,
      UUID actor,
      UUID training,
      UUID technique,
      String title,
      String objective,
      String observation,
      Priority priority,
      LocalDate reviewDate,
      Instant now) {
    require(
        reviewDate == null || !reviewDate.isBefore(LocalDate.ofInstant(now, ZoneOffset.UTC)),
        "Review date cannot be in the past.");
    return new Goal(
        UUID.randomUUID(),
        profile,
        org,
        art,
        actor,
        training,
        technique,
        title,
        objective,
        observation,
        priority,
        reviewDate,
        Status.ACTIVE,
        RecordSource.ORGANIZATION,
        now,
        now,
        0);
  }

  public enum Status {
    ACTIVE,
    ACHIEVED,
    CANCELLED
  }

  public enum Priority {
    LOW,
    NORMAL,
    HIGH
  }

  public Goal close(Status target, Instant now) {
    require(
        target == Status.ACHIEVED || target == Status.CANCELLED,
        "Goal may only close as achieved or cancelled.");
    if (status != Status.ACTIVE)
      throw com.hajime.shared.BusinessException.conflict(
          "INVALID_STATE_TRANSITION", "Only active goals can be closed.");
    return new Goal(
        id,
        practitionerProfileId,
        organizationId,
        martialArtId,
        assignedByUserId,
        sourceTrainingSessionId,
        relatedTechniqueId,
        title,
        objective,
        coachObservation,
        priority,
        reviewDate,
        target,
        source,
        createdAt,
        now,
        version + 1);
  }
}
