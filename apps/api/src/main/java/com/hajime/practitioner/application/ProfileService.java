package com.hajime.practitioner.application;

import com.hajime.identity.CurrentUser;
import com.hajime.martialart.*;
import com.hajime.practitioner.Practitioners.Profile;
import com.hajime.practitioner.infrastructure.ProfileRepository;
import com.hajime.shared.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final ProfileRepository repository;
  private final CurrentUser user;
  private final Catalog catalog;
  private final MartialArtPolicyRegistry policies;

  public ProfileService(
      ProfileRepository repository,
      CurrentUser user,
      Catalog catalog,
      MartialArtPolicyRegistry policies) {
    this.repository = repository;
    this.user = user;
    this.catalog = catalog;
    this.policies = policies;
  }

  @Transactional
  public Profile create(CreatePractitionerProfile command) {
    var actor = user.id();
    var art = catalog.art(command.martialArtCode());
    policies.resolve(new MartialArtCode(art.code()));
    var weight = command.weightKg();
    DomainValidation.require(
        weight == null
            || weight.signum() > 0
                && weight.compareTo(new BigDecimal("999.99")) <= 0
                && weight.stripTrailingZeros().scale() <= 2,
        "Weight must be positive with at most two decimal places.");
    return repository
        .create(UUID.randomUUID(), actor, art.id(), weight)
        .orElseThrow(
            () ->
                BusinessException.conflict(
                    "PROFILE_ALREADY_EXISTS",
                    "A practitioner profile already exists for this martial art."));
  }

  @Transactional(readOnly = true)
  public Profile detail(UUID id) {
    return repository
        .owned(id, user.id())
        .orElseThrow(() -> BusinessException.missing("PRACTITIONER_NOT_FOUND"));
  }
}
