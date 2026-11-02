package com.hajime.curriculum;

import java.util.UUID;

public record CurriculumRequirement(
    UUID id,
    UUID curriculumId,
    UUID rankId,
    UUID techniqueId,
    String criterionCode,
    String name,
    String description,
    boolean required) {}
