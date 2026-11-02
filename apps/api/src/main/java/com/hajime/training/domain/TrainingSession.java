package com.hajime.training.domain;

import static com.hajime.shared.DomainValidation.*;

import com.hajime.shared.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Immutable aggregate: creation rules and state transitions belong here. */
public final class TrainingSession {
  public enum Status {
    DRAFT,
    RECORDED,
    SHARED_WITH_COACH,
    REVIEWED
  }

  public enum PracticeType {
    TECHNICAL,
    SPARRING,
    CONDITIONING,
    MIXED
  }

  public record Snapshot(
      UUID id,
      UUID practitionerProfileId,
      UUID martialArtId,
      UUID organizationId,
      TrainingContent content,
      RecordSource source,
      Status status,
      Instant createdAt,
      Instant updatedAt,
      Long version) {}

  private final Snapshot data;

  private TrainingSession(Snapshot data) {
    required(data, "Training");
    required(data.id(), "Training ID");
    required(data.practitionerProfileId(), "Practitioner");
    required(data.martialArtId(), "Martial art");
    required(data.content(), "Content");
    required(data.source(), "Source");
    required(data.status(), "Status");
    required(data.createdAt(), "Creation time");
    required(data.updatedAt(), "Update time");
    require(data.version() == null || data.version() >= 0, "Invalid version.");
    require(
        data.status() != Status.SHARED_WITH_COACH && data.status() != Status.REVIEWED
            || data.organizationId() != null,
        "Shared training requires an organization.");
    this.data = data;
  }

  public static TrainingSession record(
      UUID id, UUID profile, UUID art, UUID organization, TrainingContent content, Instant now) {
    now = now.truncatedTo(ChronoUnit.MICROS);
    required(content, "Content");
    require(!content.occurredAt().isAfter(now), "Training cannot occur in the future.");
    return new TrainingSession(
        new Snapshot(
            id,
            profile,
            art,
            organization,
            content,
            RecordSource.PERSONAL,
            Status.RECORDED,
            now,
            now,
            null));
  }

  /** Persistence reconstruction preserves historical dates and provenance. */
  public static TrainingSession restore(Snapshot data) {
    return new TrainingSession(data);
  }

  public Snapshot snapshot() {
    return data;
  }

  public boolean sameRecording(TrainingSession other) {
    var d = other.data;
    return data.id().equals(d.id())
        && data.practitionerProfileId().equals(d.practitionerProfileId())
        && data.martialArtId().equals(d.martialArtId())
        && Objects.equals(data.organizationId(), d.organizationId())
        && data.source() == d.source()
        && data.content().sameRecording(d.content());
  }

  public boolean isShared() {
    return data.status() == Status.SHARED_WITH_COACH || data.status() == Status.REVIEWED;
  }

  public TrainingSession share(Instant now) {
    if (data.status() != Status.RECORDED)
      throw BusinessException.conflict(
          "INVALID_STATE_TRANSITION", "Only recorded training can be shared.");
    if (data.organizationId() == null)
      throw BusinessException.conflict(
          "ORGANIZATION_REQUIRED",
          "Choose an organization when recording training before sharing it.");
    return transition(Status.SHARED_WITH_COACH, now);
  }

  public TrainingSession review(Instant now) {
    if (data.status() != Status.SHARED_WITH_COACH)
      throw BusinessException.conflict(
          "INVALID_STATE_TRANSITION", "Only shared training can be reviewed.");
    return transition(Status.REVIEWED, now);
  }

  private TrainingSession transition(Status status, Instant now) {
    now = now.truncatedTo(ChronoUnit.MICROS);
    return new TrainingSession(
        new Snapshot(
            data.id(),
            data.practitionerProfileId(),
            data.martialArtId(),
            data.organizationId(),
            data.content(),
            data.source(),
            status,
            data.createdAt(),
            now,
            data.version()));
  }

  public TrainingSession edit(TrainingContent content, UUID organization, Instant now) {
    if (data.status() != Status.RECORDED)
      throw BusinessException.conflict(
          "INVALID_STATE_TRANSITION", "Only unshared recorded training can be edited.");
    required(content, "Content");
    now = now.truncatedTo(ChronoUnit.MICROS);
    require(!content.occurredAt().isAfter(now), "Training cannot occur in the future.");
    return new TrainingSession(
        new Snapshot(
            data.id(),
            data.practitionerProfileId(),
            data.martialArtId(),
            organization,
            content,
            data.source(),
            data.status(),
            data.createdAt(),
            now,
            data.version()));
  }
}
