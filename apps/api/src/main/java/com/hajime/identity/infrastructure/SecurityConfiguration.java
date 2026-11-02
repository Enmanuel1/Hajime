package com.hajime.identity.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
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

  private HttpSecurity base(
      HttpSecurity http, JwtDecoder decoder, AuthRateLimiter limits, AuthSettings settings)
      throws Exception {
    return http.cors(c -> {})
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, s, x) -> problem(s, 401, "UNAUTHENTICATED"))
                    .accessDeniedHandler((q, s, x) -> problem(s, 403, "ACCESS_DENIED")))
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint((q, s, x) -> problem(s, 401, "UNAUTHENTICATED"))
                    .accessDeniedHandler((q, s, x) -> problem(s, 403, "ACCESS_DENIED"))
                    .jwt(j -> j.decoder(decoder)))
        .addFilterBefore(
            new AuthRateLimitFilter(limits, settings), BearerTokenAuthenticationFilter.class);
  }

  @Bean
  @Profile("dev | test")
  SecurityFilterChain development(
      HttpSecurity http,
      Environment environment,
      JwtDecoder decoder,
      AuthRateLimiter limits,
      AuthSettings settings)
      throws Exception {
    if (environment.acceptsProfiles(Profiles.of("prod", "production")))
      throw new IllegalStateException("Development identity cannot run with a production profile.");
    return base(http, decoder, limits, settings)
        .addFilterBefore(new DevelopmentIdentityFilter(), AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/recover")
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
      HttpSecurity http, JwtDecoder decoder, AuthRateLimiter limits, AuthSettings settings)
      throws Exception {
    return base(http, decoder, limits, settings)
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/recover")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
