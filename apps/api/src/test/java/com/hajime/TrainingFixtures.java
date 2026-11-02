package com.hajime;

import com.hajime.training.TrainingView;
import com.hajime.training.domain.*;

public final class TrainingFixtures {
  private TrainingFixtures() {}

  public static TrainingSession restore(TrainingView d) {
    return TrainingSession.restore(
        new TrainingSession.Snapshot(
            d.id(),
            d.practitionerProfileId(),
            d.martialArtId(),
            d.organizationId(),
            new TrainingContent(
                d.occurredAt(),
                d.durationMinutes(),
                TrainingSession.PracticeType.valueOf(d.practiceType()),
                d.perceivedIntensity(),
                d.energyLevel(),
                d.reflection(),
                d.techniques().stream()
                    .map(
                        v ->
                            new TrainingContent.TechniqueEntry(
                                v.techniqueId(), v.repetitions(), v.durationMinutes(), v.notes()))
                    .toList(),
                d.sparring().stream()
                    .map(
                        v ->
                            new TrainingContent.SparringEntry(v.rounds(), v.intensity(), v.notes()))
                    .toList()),
            d.source(),
            TrainingSession.Status.valueOf(d.status()),
            d.createdAt(),
            d.updatedAt(),
            d.version()));
  }
}
