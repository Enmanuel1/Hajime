package com.hajime.training.infrastructure.persistence;

import com.hajime.shared.RecordSource;
import com.hajime.training.domain.TrainingContent;
import com.hajime.training.domain.TrainingSession;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "training_session")
public class TrainingJpaEntity {
  @Id UUID id;
  UUID practitionerProfileId;
  UUID martialArtId;
  UUID organizationId;
  Instant occurredAt;
  int durationMinutes;
  String practiceType;
  Integer perceivedIntensity;
  Integer energyLevel;
  String reflection;

  @Enumerated(EnumType.STRING)
  RecordSource source;

  String status;
  Instant createdAt;
  Instant updatedAt;
  @Version Long version;

  @ElementCollection
  @CollectionTable(
      name = "training_technique",
      joinColumns = @JoinColumn(name = "training_session_id"))
  @org.hibernate.annotations.BatchSize(size = 100)
  List<TechniqueValue> techniques = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "sparring_entry", joinColumns = @JoinColumn(name = "training_session_id"))
  @OrderColumn(name = "entry_index")
  @org.hibernate.annotations.BatchSize(size = 100)
  List<SparringValue> sparring = new ArrayList<>();

  protected TrainingJpaEntity() {}

  static TrainingJpaEntity from(TrainingSession aggregate) {
    var d = aggregate.snapshot();
    var c = d.content();
    var e = new TrainingJpaEntity();
    e.id = d.id();
    e.practitionerProfileId = d.practitionerProfileId();
    e.martialArtId = d.martialArtId();
    e.organizationId = d.organizationId();
    e.occurredAt = c.occurredAt();
    e.durationMinutes = c.durationMinutes();
    e.practiceType = c.practiceType().name();
    e.perceivedIntensity = c.perceivedIntensity();
    e.energyLevel = c.energyLevel();
    e.reflection = c.reflection();
    e.source = d.source();
    e.status = d.status().name();
    e.createdAt = d.createdAt();
    e.updatedAt = d.updatedAt();
    e.version = d.version();
    e.techniques =
        c.techniques().stream()
            .map(TechniqueValue::new)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    e.sparring =
        c.sparring().stream()
            .map(SparringValue::new)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    return e;
  }

  TrainingSession domain() {
    return TrainingSession.restore(
        new TrainingSession.Snapshot(
            id,
            practitionerProfileId,
            martialArtId,
            organizationId,
            new TrainingContent(
                occurredAt,
                durationMinutes,
                TrainingSession.PracticeType.valueOf(practiceType),
                perceivedIntensity,
                energyLevel,
                reflection,
                techniques.stream().map(TechniqueValue::domain).toList(),
                sparring.stream().map(SparringValue::domain).toList()),
            source,
            TrainingSession.Status.valueOf(status),
            createdAt,
            updatedAt,
            version));
  }

  @Embeddable
  public static class TechniqueValue {
    UUID techniqueId;
    Integer repetitions;
    Integer durationMinutes;
    String notes;

    protected TechniqueValue() {}

    TechniqueValue(TrainingContent.TechniqueEntry d) {
      techniqueId = d.techniqueId();
      repetitions = d.repetitions();
      durationMinutes = d.durationMinutes();
      notes = d.notes();
    }

    TrainingContent.TechniqueEntry domain() {
      return new TrainingContent.TechniqueEntry(techniqueId, repetitions, durationMinutes, notes);
    }
  }

  @Embeddable
  public static class SparringValue {
    Integer rounds;
    Integer intensity;
    String notes;

    protected SparringValue() {}

    SparringValue(TrainingContent.SparringEntry d) {
      rounds = d.rounds();
      intensity = d.intensity();
      notes = d.notes();
    }

    TrainingContent.SparringEntry domain() {
      return new TrainingContent.SparringEntry(rounds, intensity, notes);
    }
  }
}
