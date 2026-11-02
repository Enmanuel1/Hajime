package com.hajime.coaching.domain;

import static com.hajime.shared.DomainValidation.*;

import java.time.Instant;
import java.util.UUID;

public record Feedback(
    UUID id,
    UUID reviewId,
    UUID trainingSessionId,
    UUID practitionerProfileId,
    UUID authorUserId,
    UUID organizationId,
    String body,
    Instant createdAt) {
  public Feedback {
    required(id, "Feedback ID");
    required(reviewId, "Review");
    required(trainingSessionId, "Training");
    required(practitionerProfileId, "Practitioner");
    required(authorUserId, "Author");
    required(organizationId, "Organization");
    required(createdAt, "Creation time");
    body = text(body, 4000, "Feedback body");
  }

  public static Feedback create(TrainingReview review, UUID author, String body, Instant now) {
    return new Feedback(
        UUID.randomUUID(),
        review.id(),
        review.trainingSessionId(),
        review.practitionerProfileId(),
        author,
        review.organizationId(),
        body,
        now);
  }
}
