package com.hajime.training.infrastructure.persistence;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTrainingRepository extends JpaRepository<TrainingJpaEntity, UUID> {
  Optional<TrainingJpaEntity> findByIdAndPractitionerProfileIdIn(UUID id, List<UUID> profileIds);

  Optional<TrainingJpaEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<TrainingJpaEntity> findByPractitionerProfileIdIn(List<UUID> profileIds, Pageable page);
}
