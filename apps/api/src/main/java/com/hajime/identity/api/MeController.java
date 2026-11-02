package com.hajime.identity.api;

import com.hajime.identity.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController {
  private final UserDirectory directory;

  public MeController(UserDirectory directory) {
    this.directory = directory;
  }

  @GetMapping("/api/v1/me")
  public UserDirectory.User me(Authentication auth) {
    return directory.bySubject(auth.getName());
  }
}
