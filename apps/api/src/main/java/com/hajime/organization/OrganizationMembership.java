package com.hajime.organization;

import java.util.UUID;

public record OrganizationMembership(
    UUID id, UUID organizationId, UUID userId, MembershipRole role, boolean active) {}
