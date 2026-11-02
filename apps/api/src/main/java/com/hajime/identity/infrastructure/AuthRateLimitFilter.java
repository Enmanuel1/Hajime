package com.hajime.identity.infrastructure;

import com.hajime.shared.BusinessException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

final class AuthRateLimitFilter extends OncePerRequestFilter {
  private final AuthRateLimiter limits;
  private final AuthSettings settings;

  AuthRateLimitFilter(AuthRateLimiter limits, AuthSettings settings) {
    this.limits = limits;
    this.settings = settings;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/v1/auth/")) {
      try {
        limits.ip(request.getRemoteAddr());
      } catch (BusinessException e) {
        if (e.kind() != BusinessException.Kind.RATE_LIMITED) throw e;
        response.setHeader("Retry-After", Long.toString(settings.rateWindow().toSeconds()));
        SecurityConfiguration.problem(response, 429, "RATE_LIMITED");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
