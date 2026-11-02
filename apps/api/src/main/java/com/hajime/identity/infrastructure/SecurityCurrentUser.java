package com.hajime.identity.infrastructure;

import com.hajime.identity.*;
import com.hajime.shared.BusinessException;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUser implements CurrentUser {
  private final UserDirectory directory;

  public SecurityCurrentUser(UserDirectory directory) {
    this.directory = directory;
  }

  public UUID id() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken)
      throw BusinessException.unauthenticated("UNAUTHENTICATED", "Authentication required.");
    return directory.bySubject(authentication.getName()).id();
  }
}
