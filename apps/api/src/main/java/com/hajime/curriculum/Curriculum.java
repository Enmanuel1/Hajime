package com.hajime.curriculum;

import java.util.UUID;

/** Organization may override a versioned martial-art curriculum; null organization means global. */
public record Curriculum(
    UUID id, UUID martialArtId, UUID organizationId, String name, int version, boolean active) {}
