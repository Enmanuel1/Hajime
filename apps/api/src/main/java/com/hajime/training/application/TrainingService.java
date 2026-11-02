package com.hajime.training.application;

import com.hajime.identity.CurrentUser;
import com.hajime.martialart.*;
import com.hajime.organization.OrganizationAccess;
import com.hajime.practitioner.Practitioners;
import com.hajime.shared.*;
import com.hajime.training.*;
import com.hajime.training.domain.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {
  private final TrainingRepository repository;
  private final CurrentUser user;
  private final Practitioners practitioners;
  private final OrganizationAccess access;
  private final Catalog catalog;
  private final MartialArtPolicyRegistry policies;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public TrainingService(
      TrainingRepository repository,
      CurrentUser user,
      Practitioners practitioners,
      OrganizationAccess access,
      Catalog catalog,
      MartialArtPolicyRegistry policies,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.user = user;
    this.practitioners = practitioners;
    this.access = access;
    this.catalog = catalog;
    this.policies = policies;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public TrainingView record(RecordTraining command) {
    var actor = user.id();
    var profile = practitioners.requireOwned(command.practitionerProfileId(), actor);
    policies.resolve(new MartialArtCode(catalog.art(profile.martialArtId()).code()));
    if (command.organizationId() != null) {
      access.requireMember(command.organizationId(), actor);
      access.requireMartialArt(command.organizationId(), profile.martialArtId());
    }
    var content = command.toContent();
    catalog.requireTechniques(
        profile.martialArtId(),
        content.techniques().stream().map(TrainingContent.TechniqueEntry::techniqueId).toList());
    var session =
        TrainingSession.record(
            command.id() == null ? UUID.randomUUID() : command.id(),
            profile.id(),
            profile.martialArtId(),
            command.organizationId(),
            content,
            clock.instant());
    return TrainingViews.from(repository.create(session));
  }

  @Transactional(readOnly = true)
  public TrainingView detail(UUID id) {
    return TrainingViews.from(owned(id, user.id()));
  }

  @Transactional(readOnly = true)
  public PageResult<TrainingView> history(int page, int size, String sort) {
    PageResult.limit(page, size);
    if (!Set.of("occurredAt,desc", "occurredAt,asc").contains(sort))
      throw BusinessException.invalid(
          "INVALID_SORT", "Supported sort: occurredAt,asc or occurredAt,desc.");
    var result = repository.history(profiles(), page, size, sort.endsWith("asc"));
    return new PageResult<>(
        result.content().stream().map(TrainingViews::from).toList(),
        page,
        size,
        result.totalElements());
  }

  @Transactional
  public TrainingView share(UUID id, long expectedVersion) {
    var actor = user.id();
    var session = owned(id, actor);
    practitioners.requireOwned(session.snapshot().practitionerProfileId(), actor);
    if (!Objects.equals(session.snapshot().version(), expectedVersion))
      throw BusinessException.conflict(
          "CONCURRENT_MODIFICATION", "Reload the training session before sharing.");
    var org = session.snapshot().organizationId();
    if (org != null) {
      access.requireMember(org, actor);
      access.requireMartialArt(org, session.snapshot().martialArtId());
    }
    var shared = TrainingViews.from(repository.save(session.share(clock.instant())));
    events.publishEvent(
        new TrainingSessionShared(
            shared.id(),
            shared.practitionerProfileId(),
            shared.martialArtId(),
            shared.organizationId(),
            actor));
    return shared;
  }

  private List<UUID> profiles() {
    return practitioners.ownedBy(user.id()).stream().map(Practitioners.Profile::id).toList();
  }

  private TrainingSession owned(UUID id, UUID actor) {
    return repository
        .owned(id, practitioners.ownedBy(actor).stream().map(Practitioners.Profile::id).toList())
        .orElseThrow(() -> BusinessException.missing("TRAINING_NOT_FOUND"));
  }

  @Transactional
  public TrainingView edit(UUID id, UpdateTraining command) {
    var actor = user.id();
    var session = owned(id, actor);
    var before = session.snapshot();
    practitioners.requireOwned(before.practitionerProfileId(), actor);
    if (!Objects.equals(before.version(), command.version()))
      throw BusinessException.conflict("CONCURRENT_MODIFICATION", "Reload training before saving.");
    policies.resolve(new MartialArtCode(catalog.art(before.martialArtId()).code()));
    if (command.organizationId() != null) {
      access.requireMember(command.organizationId(), actor);
      access.requireMartialArt(command.organizationId(), before.martialArtId());
    }
    var content = command.toContent();
    catalog.requireTechniques(
        before.martialArtId(),
        content.techniques().stream().map(TrainingContent.TechniqueEntry::techniqueId).toList());
    return TrainingViews.from(
        repository.save(session.edit(content, command.organizationId(), clock.instant())));
  }
}
