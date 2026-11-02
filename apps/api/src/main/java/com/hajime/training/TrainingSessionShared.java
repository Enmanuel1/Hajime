package com.hajime.training;

import java.util.UUID;

public record TrainingSessionShared(
    UUID trainingSessionId,
    UUID practitionerProfileId,
    UUID martialArtId,
    UUID organizationId,
    UUID actorUserId) {}
