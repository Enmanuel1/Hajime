package com.hajime.training.application;

import com.hajime.training.TrainingView;
import com.hajime.training.domain.TrainingSession;

/** Maps domain snapshots to the stable public response contract. */
public final class TrainingViews {
  private TrainingViews() {}

  public static TrainingView from(TrainingSession session) {
    var d = session.snapshot();
    var c = d.content();
    return new TrainingView(
        d.id(),
        d.practitionerProfileId(),
        d.martialArtId(),
        d.organizationId(),
        c.occurredAt(),
        c.durationMinutes(),
        c.practiceType().name(),
        c.perceivedIntensity(),
        c.energyLevel(),
        c.reflection(),
        d.source(),
        d.status().name(),
        d.createdAt(),
        d.updatedAt(),
        d.version(),
        c.techniques().stream()
            .map(
                t ->
                    new TrainingView.TechniqueEntry(
                        t.techniqueId(), t.repetitions(), t.durationMinutes(), t.notes()))
            .toList(),
        c.sparring().stream()
            .map(s -> new TrainingView.SparringEntry(s.rounds(), s.intensity(), s.notes()))
            .toList());
  }
}
