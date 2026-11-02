package com.hajime.training.domain;

import com.hajime.shared.PageResult;
import java.util.*;

public interface TrainingRepository {
  TrainingSession create(TrainingSession session);

  TrainingSession save(TrainingSession session);

  Optional<TrainingSession> owned(UUID id, List<UUID> profileIds);

  Optional<TrainingSession> inOrganization(UUID id, UUID organizationId);

  PageResult<TrainingSession> history(List<UUID> profileIds, int page, int size, boolean ascending);
}
