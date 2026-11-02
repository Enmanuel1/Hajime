package com.hajime.coaching.domain;

import com.hajime.shared.ApiException;
import java.time.Instant;
import java.util.UUID;

public record TrainingReview(
    UUID id,
    UUID trainingSessionId,
    UUID practitionerProfileId,
    UUID organizationId,
    UUID martialArtId,
    Status status,
    Instant createdAt,
    Instant reviewedAt,
    long version) {
  public enum Status {
    PENDING,
    REVIEWED
  }

  public TrainingReview reviewed(Instant now) {
    if (status != Status.PENDING)
      throw new ApiException(
          409, "INVALID_STATE_TRANSITION", "This training has already been reviewed.");
    return new TrainingReview(
        id,
        trainingSessionId,
        practitionerProfileId,
        organizationId,
        martialArtId,
        Status.REVIEWED,
        createdAt,
        now,
        version + 1);
  }
}
