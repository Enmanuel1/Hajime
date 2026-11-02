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
      assertThatThrownBy(
              () -> new SecurityConfiguration().development(null, environment, null, null, null))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void productionRequiresAnExplicitSigningKey() {
    assertThatThrownBy(() -> SigningKeys.resolve("", false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void invalidOrWeakKeysAreRejectedAndDevelopmentKeysAreEphemeral() {
    assertThatThrownBy(() -> SigningKeys.resolve("invalid", false))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                SigningKeys.resolve(
                    java.util.Base64.getEncoder().encodeToString(new byte[16]), false))
        .isInstanceOf(IllegalStateException.class);
    assertThat(SigningKeys.resolve("", true).getEncoded())
        .isNotEqualTo(SigningKeys.resolve("", true).getEncoded());
  }
}
