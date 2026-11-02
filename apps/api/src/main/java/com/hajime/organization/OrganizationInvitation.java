package com.hajime.organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationInvitation(
    UUID id,
    UUID organizationId,
    UUID recipientUserId,
    MembershipRole role,
    Instant expiresAt,
    String status) {
  public boolean canAccept(Instant now) {
    return status.equals("PENDING") && expiresAt.isAfter(now);
  }
}
