package com.hajime.identity.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keep consumed refresh history until after absolute session expiry; remove at most 1,000 families.
 */
@Component
class SessionMaintenance {
  private final AuthRepository accounts;

  SessionMaintenance(AuthRepository accounts) {
    this.accounts = accounts;
  }

  @Scheduled(
      fixedDelayString = "${hajime.auth.cleanup-delay-ms:3600000}",
      initialDelayString = "${hajime.auth.cleanup-initial-delay-ms:3600000}")
  public void purgeExpired() {
    accounts.purgeExpiredSessions();
  }
}
