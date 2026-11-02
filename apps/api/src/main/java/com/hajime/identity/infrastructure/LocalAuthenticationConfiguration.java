package com.hajime.identity.infrastructure;

import java.time.Clock;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@EnableConfigurationProperties(AuthSettings.class)
public class LocalAuthenticationConfiguration {
  @Bean
  SecretKey localJwtKey(AuthSettings settings, Environment env) {
    return SigningKeys.resolve(settings.secret(), env.acceptsProfiles(Profiles.of("dev", "test")));
  }

  @Bean
  PasswordEncoder localPasswords() {
    var pbkdf =
        new Pbkdf2PasswordEncoder(
            "", 16, 600000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    return new DelegatingPasswordEncoder("pbkdf2-sha256-v1", Map.of("pbkdf2-sha256-v1", pbkdf));
  }

  @Bean
  JwtDecoder localJwtDecoder(
      SecretKey localJwtKey, AuthSettings settings, AuthRepository accounts, Clock clock) {
    var decoder =
        NimbusJwtDecoder.withSecretKey(localJwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(settings.issuer()),
            new SessionTokenValidator(settings, accounts, clock)));
    return decoder;
  }
}
