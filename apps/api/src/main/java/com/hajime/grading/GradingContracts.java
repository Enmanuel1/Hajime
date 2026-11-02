package com.hajime.grading;

import java.util.UUID;

/** Reserved concepts. Eligibility will delegate to martial-art policies and versioned curricula. */
public final class GradingContracts {
  private GradingContracts() {}

  public record Evaluation(
      UUID id,
      UUID practitionerProfileId,
      UUID organizationId,
      UUID targetRankId,
      UUID curriculumId) {}

  public record EvaluationCriterion(
      UUID id, UUID evaluationId, UUID curriculumRequirementId, boolean passed) {}

  public record PromotionProposal(UUID id, UUID evaluationId, UUID proposedByUserId) {}

  public record Promotion(UUID id, UUID proposalId, UUID approvedByUserId) {}
}
