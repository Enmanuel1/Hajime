package com.hajime.identity.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Constructed only by the dev/test SecurityFilterChain, never component scanned. */
final class DevelopmentIdentityFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getHeader("Authorization") != null
        || request.getRequestURI().startsWith("/api/v1/auth/")) {
      chain.doFilter(request, response);
      return;
    }
    String alias = request.getHeader("X-Dev-User");
    if (alias == null) alias = "athlete";
    if (!List.of("athlete", "sensei", "outsider").contains(alias)) {
      SecurityConfiguration.problem(response, 401, "UNAUTHENTICATED");
      return;
    }
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken("dev:" + alias, null, List.of()));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
