package com.hajime.coaching.application;

import com.hajime.coaching.domain.Goal.Priority;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record AssignGoal(
    UUID sourceTrainingSessionId,
    UUID relatedTechniqueId,
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 2000) String objective,
    @Size(max = 2000) String coachObservation,
    @NotNull Priority priority,
    LocalDate reviewDate) {}
