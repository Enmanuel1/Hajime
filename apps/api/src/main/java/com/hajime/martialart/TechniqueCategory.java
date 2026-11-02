package com.hajime.martialart;

import java.util.UUID;

public record TechniqueCategory(
    UUID id, UUID martialArtId, String code, String name, int orderIndex) {}
