package com.hajime.coaching;

import java.util.UUID;

public record FeedbackCreated(
    UUID feedbackId,
    UUID trainingSessionId,
    UUID practitionerProfileId,
    UUID organizationId,
    UUID actorUserId) {}
