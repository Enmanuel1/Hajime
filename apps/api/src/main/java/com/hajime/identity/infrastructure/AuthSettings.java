package com.hajime.identity.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("hajime.auth")
public record AuthSettings(
    @DefaultValue("") String secret,
    @DefaultValue("hajime") String issuer,
    @DefaultValue("hajime-app") String audience,
    @DefaultValue("PT15M") Duration accessTtl,
    @DefaultValue("P30D") Duration sessionTtl,
    @DefaultValue("PT15M") Duration rateWindow,
    @DefaultValue("100") int maxIpAttempts,
    @DefaultValue("10") int maxAccountAttempts) {
  public AuthSettings {
    if (issuer == null
        || issuer.isBlank()
        || audience == null
        || audience.isBlank()
        || accessTtl == null
        || accessTtl.toSeconds() < 10
        || accessTtl.compareTo(Duration.ofHours(1)) > 0
        || sessionTtl == null
        || sessionTtl.compareTo(accessTtl) <= 0
        || sessionTtl.compareTo(Duration.ofDays(90)) > 0
        || rateWindow == null
        || rateWindow.toSeconds() < 1
        || rateWindow.compareTo(Duration.ofHours(24)) > 0
        || maxIpAttempts < 1
        || maxAccountAttempts < 1)
      throw new IllegalArgumentException("Invalid local authentication settings.");
  }

  @Override
  public String toString() {
    return "AuthSettings[secret=REDACTED]";
  }
}
