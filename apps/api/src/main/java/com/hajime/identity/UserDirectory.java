package com.hajime.identity;

import com.hajime.identity.infrastructure.UserRepository;
import com.hajime.shared.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class UserDirectory {
  public record User(UUID id, String displayName) {}

  private final UserRepository repository;

  public UserDirectory(UserRepository repository) {
    this.repository = repository;
  }

  public User bySubject(String subject) {
    return repository
        .bySubject(subject)
        .orElseThrow(
            () ->
                new ApiException(
                    401, "UNAUTHENTICATED", "Authenticated identity has not been provisioned."));
  }
}
