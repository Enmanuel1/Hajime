package com.hajime.identity.infrastructure;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigurationTest {
  @Test
  void productionAndDevelopmentProfilesCannotBeCombined() {
    for (var name : new String[] {"prod", "production"}) {
      var environment = new MockEnvironment();
      environment.setActiveProfiles("dev", name);
      assertThatThrownBy(() -> new SecurityConfiguration().development(null, environment))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void configuredDecoderRequiresAnExplicitClient() {
    assertThatThrownBy(() -> new AccessTokenPolicy(" ")).isInstanceOf(IllegalStateException.class);
  }
}
