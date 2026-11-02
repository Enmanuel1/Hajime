package com.hajime.organization;

import com.hajime.organization.infrastructure.MembershipRepository;
import com.hajime.shared.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAccess {
  private final MembershipRepository memberships;

  public OrganizationAccess(MembershipRepository memberships) {
    this.memberships = memberships;
  }

  public void requireMember(UUID organization, UUID user) {
    if (memberships.roles(organization, user).isEmpty()) throw ApiException.denied();
  }

  public void requirePermission(UUID organization, UUID user, Permission permission) {
    if (memberships.roles(organization, user).stream().noneMatch(r -> r.permits(permission)))
      throw ApiException.denied();
  }

  public void requireMartialArt(UUID organization, UUID art) {
    if (!memberships.supports(organization, art))
      throw new ApiException(
          400,
          "ORGANIZATION_MARTIAL_ART_MISMATCH",
          "Organization does not support this martial art.");
  }
}
