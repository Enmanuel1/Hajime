package com.hajime.organization;

import java.util.UUID;

public record OrganizationChanged(
    UUID actorUserId, UUID organizationId, String action, String entityType, UUID entityId) {}
