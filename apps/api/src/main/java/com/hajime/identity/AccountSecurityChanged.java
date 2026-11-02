package com.hajime.identity;

import java.util.UUID;

public record AccountSecurityChanged(UUID userId, String action) {}
