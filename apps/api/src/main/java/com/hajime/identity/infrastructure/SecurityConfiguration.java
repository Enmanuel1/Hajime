package com.hajime.identity.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
    type =
        org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
  static void problem(HttpServletResponse response, int status, String code) throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\""
                + code
                + "\",\"status\":"
                + status
                + ",\"code\":\""
                + code
                + "\"}");
  }

  private HttpSecurity base(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, s, x) -> problem(s, 401, "UNAUTHENTICATED"))
                    .accessDeniedHandler((q, s, x) -> problem(s, 403, "ACCESS_DENIED")));
  }

  @Bean
  @Profile("dev | test")
  SecurityFilterChain development(HttpSecurity http, Environment environment) throws Exception {
    if (environment.acceptsProfiles(Profiles.of("prod", "production")))
      throw new IllegalStateException("Development identity cannot run with a production profile.");
    return base(http)
        .addFilterBefore(new DevelopmentIdentityFilter(), AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }

  @Bean
  @Profile("!dev & !test")
  SecurityFilterChain production(
      HttpSecurity http,
      ObjectProvider<JwtDecoder> decoder,
      @Value("${hajime.security.jwt.client-id:}") String clientId)
      throws Exception {
    base(http)
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    if (decoder.getIfAvailable() != null)
      http.oauth2ResourceServer(
          o ->
              o.authenticationEntryPoint((q, s, x) -> problem(s, 401, "UNAUTHENTICATED"))
                  .accessDeniedHandler((q, s, x) -> problem(s, 403, "ACCESS_DENIED"))
                  .jwt(j -> j.decoder(new AccessTokenDecoder(decoder.getObject(), clientId))));
    return http.build();
  }
}
