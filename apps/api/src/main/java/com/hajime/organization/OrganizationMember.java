package com.hajime.organization;

import java.util.UUID;

public record OrganizationMember(
    UUID id, UUID userId, String displayName, MembershipRole role, boolean active) {}
