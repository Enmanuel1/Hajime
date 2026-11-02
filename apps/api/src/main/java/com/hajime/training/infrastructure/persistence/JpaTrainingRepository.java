package com.hajime.training.infrastructure.persistence;

import com.hajime.shared.BusinessException;
import com.hajime.shared.PageResult;
import com.hajime.training.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTrainingRepository implements TrainingRepository {
  private final SpringDataTrainingRepository repository;
  private final JdbcClient db;

  public JpaTrainingRepository(SpringDataTrainingRepository repository, JdbcClient db) {
    this.repository = repository;
    this.db = db;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public TrainingSession create(TrainingSession session) {
    var id = session.snapshot().id();
    // Held until commit, including across API instances. Hash collisions only serialize unrelated
    // IDs.
    db.sql("select pg_advisory_xact_lock(hashtextextended(:id, 0))")
        .param("id", id.toString())
        .query((rs, row) -> true)
        .single();
    var existing = repository.findById(id).map(TrainingJpaEntity::domain);
    if (existing.isPresent()) {
      if (!existing.get().sameRecording(session))
        throw BusinessException.conflict(
            "TRAINING_ID_CONFLICT", "Training ID conflicts with an existing recording.");
      return existing.get();
    }
    return save(session);
  }

  public TrainingSession save(TrainingSession session) {
    return repository.saveAndFlush(TrainingJpaEntity.from(session)).domain();
  }

  public Optional<TrainingSession> owned(UUID id, List<UUID> profiles) {
    return repository
        .findByIdAndPractitionerProfileIdIn(id, profiles)
        .map(TrainingJpaEntity::domain);
  }

  public Optional<TrainingSession> inOrganization(UUID id, UUID org) {
    return repository.findByIdAndOrganizationId(id, org).map(TrainingJpaEntity::domain);
  }

  public PageResult<TrainingSession> history(
      List<UUID> profiles, int page, int size, boolean ascending) {
    var sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "occurredAt", "id");
    var result =
        repository.findByPractitionerProfileIdIn(profiles, PageRequest.of(page, size, sort));
    return new PageResult<>(
        result.getContent().stream().map(TrainingJpaEntity::domain).toList(),
        page,
        size,
        result.getTotalElements());
  }
}
