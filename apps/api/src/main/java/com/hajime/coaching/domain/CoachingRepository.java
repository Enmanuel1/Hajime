package com.hajime.coaching.domain;

import com.hajime.shared.PageResult;
import java.util.*;

public interface CoachingRepository {
  void addReview(TrainingReview review);

  Optional<TrainingReview> review(UUID organization, UUID id);

  PageResult<TrainingReview> reviews(UUID organization, String status, int page, int size);

  void complete(TrainingReview original, TrainingReview reviewed);

  void addFeedback(Feedback feedback);

  List<Feedback> feedbackForReview(UUID organization, UUID reviewId);

  PageResult<Feedback> feedbackForAthlete(List<UUID> profiles, UUID session, int page, int size);

  void addGoal(Goal goal);

  PageResult<Goal> goals(List<UUID> profiles, Goal.Status status, int page, int size);

  Optional<Goal> goal(List<UUID> profiles, UUID id);

  Optional<Goal> goalInOrganization(UUID organization, UUID id);

  Goal closeGoal(Goal before, Goal after);
}
