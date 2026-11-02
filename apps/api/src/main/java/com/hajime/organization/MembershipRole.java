package com.hajime.organization;

import java.util.Set;

public enum MembershipRole {
  PRACTITIONER(Set.of()),
  INSTRUCTOR(
      Set.of(
          Permission.VIEW_STUDENTS,
          Permission.REVIEW_TRAINING,
          Permission.ASSIGN_GOAL,
          Permission.PASS_ATTENDANCE,
          Permission.CREATE_CLASS)),
  SENSEI(
      Set.of(
          Permission.VIEW_STUDENTS,
          Permission.REVIEW_TRAINING,
          Permission.ASSIGN_GOAL,
          Permission.PASS_ATTENDANCE,
          Permission.CREATE_CLASS,
          Permission.EVALUATE_RANK,
          Permission.PROPOSE_PROMOTION)),
  DIRECTOR(Set.of(Permission.values())),
  ADMIN(Set.of(Permission.values()));
  private final Set<Permission> permissions;

  MembershipRole(Set<Permission> permissions) {
    this.permissions = permissions;
  }

  public boolean permits(Permission permission) {
    return permissions.contains(permission);
  }
}
