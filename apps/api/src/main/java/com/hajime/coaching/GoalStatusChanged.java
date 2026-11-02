package com.hajime.coaching;

import java.util.UUID;

public record GoalStatusChanged(
    UUID goalId, UUID organizationId, UUID actorUserId, String status) {}
