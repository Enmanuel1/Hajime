package com.hajime.shared.web;

import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;

@Configuration
public class ApiCorsConfiguration {
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${hajime.web.cors-allowed-origins:}") String raw) {
    var origins =
        Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    for (var origin : origins) {
      var uri = URI.create(origin);
      if (!Set.of("http", "https").contains(Objects.toString(uri.getScheme(), ""))
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null
          || !Objects.toString(uri.getRawPath(), "").isEmpty())
        throw new IllegalArgumentException(
            "CORS origins must be explicit HTTP(S) origins without path or wildcard.");
    }
    var config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
