package com.hajime.organization.application;

import com.hajime.identity.CurrentUser;
import com.hajime.martialart.Catalog;
import com.hajime.organization.*;
import com.hajime.organization.infrastructure.OrganizationRepository;
import com.hajime.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organization writes serialize on the organization row, including permission checks. */
@Service
@Transactional
public class OrganizationService {
  private final OrganizationRepository repository;
  private final CurrentUser user;
  private final OrganizationAccess access;
  private final Catalog catalog;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  public OrganizationService(
      OrganizationRepository repository,
      CurrentUser user,
      OrganizationAccess access,
      Catalog catalog,
      Clock clock,
      ApplicationEventPublisher events) {
    this.repository = repository;
    this.user = user;
    this.access = access;
    this.catalog = catalog;
    this.clock = clock;
    this.events = events;
  }

  public Organization create(String name, String martialArtCode) {
    var actor = user.id();
    DomainValidation.require(
        name != null && !name.isBlank() && name.strip().length() <= 160,
        "Organization name must contain 1..160 characters.");
    var art = catalog.art(martialArtCode);
    var org = new Organization(UUID.randomUUID(), name.strip());
    repository.create(org, art.id());
    repository.add(org.id(), actor, MembershipRole.DIRECTOR);
    changed(actor, org.id(), "ORGANIZATION_CREATED", org.id());
    return org;
  }

  @Transactional(readOnly = true)
  public PageResult<Organization> mine(int page, int size) {
    PageResult.limit(page, size);
    return repository.mine(user.id(), page, size);
  }

  @Transactional(readOnly = true)
  public PageResult<OrganizationMember> members(UUID org, int page, int size) {
    PageResult.limit(page, size);
    access.requirePermission(org, user.id(), Permission.VIEW_STUDENTS);
    return repository.members(org, page, size);
  }

  public OrganizationInvitation invite(UUID org, UUID recipient, MembershipRole role) {
    var actor = user.id();
    lock(org);
    access.requirePermission(org, actor, Permission.MANAGE_MEMBERS);
    DomainValidation.require(
        role == MembershipRole.PRACTITIONER
            || role == MembershipRole.INSTRUCTOR
            || role == MembershipRole.SENSEI,
        "Invitations cannot grant administrator or director privileges.");
    DomainValidation.require(
        recipient != null && !actor.equals(recipient),
        "A recipient other than yourself is required.");
    if (!repository.userExists(recipient)) throw BusinessException.missing("RECIPIENT_NOT_FOUND");
    if (repository.activeRole(org, recipient, role))
      throw BusinessException.conflict("ALREADY_MEMBER", "This role is already active.");
    repository.cancelPending(org, recipient);
    var created = clock.instant();
    var invitation =
        new OrganizationInvitation(
            UUID.randomUUID(), org, recipient, role, created.plus(Duration.ofDays(7)), "PENDING");
    repository.invite(invitation, actor, created);
    changed(actor, org, "MEMBER_INVITED", invitation.id());
    return invitation;
  }

  @Transactional(readOnly = true)
  public PageResult<OrganizationInvitation> invitations(int page, int size) {
    PageResult.limit(page, size);
    return repository.invitations(user.id(), clock.instant(), page, size);
  }

  public void accept(UUID id) {
    var actor = user.id();
    var initial = ownedInvitation(id, actor);
    lock(initial.organizationId());
    var invitation = ownedInvitation(id, actor);
    if (!invitation.canAccept(clock.instant()))
      throw BusinessException.conflict(
          "INVITATION_UNAVAILABLE", "Invitation was consumed, revoked or expired.");
    repository.add(invitation.organizationId(), actor, invitation.role());
    repository.accepted(id);
    changed(actor, invitation.organizationId(), "INVITATION_ACCEPTED", id);
  }

  public void revoke(UUID org, UUID member) {
    var actor = user.id();
    lock(org);
    access.requirePermission(org, actor, Permission.MANAGE_MEMBERS);
    var target =
        repository
            .member(org, member)
            .orElseThrow(() -> BusinessException.missing("MEMBERSHIP_NOT_FOUND"));
    if (target.role() == MembershipRole.ADMIN || target.role() == MembershipRole.DIRECTOR)
      throw BusinessException.forbidden(
          "PROTECTED_MEMBERSHIP",
          "Director and administrator membership requires a separate ownership transfer process.");
    repository.revoke(member);
    repository.cancelPending(org, target.userId());
    if (target.active()) changed(actor, org, "MEMBERSHIP_REVOKED", member);
  }

  private OrganizationInvitation ownedInvitation(UUID id, UUID actor) {
    return repository
        .invitation(id, actor)
        .orElseThrow(() -> BusinessException.missing("INVITATION_NOT_FOUND"));
  }

  private void lock(UUID org) {
    if (!repository.lock(org)) throw BusinessException.denied();
  }

  private void changed(UUID actor, UUID org, String action, UUID id) {
    events.publishEvent(
        new OrganizationChanged(
            actor,
            org,
            action,
            switch (action) {
              case "ORGANIZATION_CREATED" -> "Organization";
              case "MEMBERSHIP_REVOKED" -> "OrganizationMembership";
              default -> "OrganizationInvitation";
            },
            id));
  }
}
