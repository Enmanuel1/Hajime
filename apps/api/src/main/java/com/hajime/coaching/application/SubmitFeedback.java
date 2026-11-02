package com.hajime.coaching.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SubmitFeedback(
    @Min(0) long version, @NotBlank @Size(max = 4000) String body, @Valid AssignGoal goal) {}
