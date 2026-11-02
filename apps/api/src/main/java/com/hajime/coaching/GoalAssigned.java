package com.hajime.coaching;

import java.util.UUID;

public record GoalAssigned(
    UUID goalId, UUID practitionerProfileId, UUID organizationId, UUID actorUserId) {}
