package com.hajime.practitioner;

import com.hajime.practitioner.infrastructure.ProfileRepository;
import com.hajime.shared.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class Practitioners {
  public record Profile(
      UUID id,
      UUID userId,
      UUID martialArtId,
      UUID currentRankId,
      BigDecimal weightKg,
      String status) {}

  private final ProfileRepository repository;

  public Practitioners(ProfileRepository repository) {
    this.repository = repository;
  }

  public List<Profile> ownedBy(UUID user) {
    return repository.ownedBy(user);
  }

  public Profile require(UUID id) {
    return repository
        .find(id)
        .filter(p -> p.status().equals("ACTIVE"))
        .orElseThrow(() -> BusinessException.missing("PRACTITIONER_NOT_FOUND"));
  }

  public Profile requireOwned(UUID id, UUID user) {
    var p = require(id);
    if (!p.userId().equals(user)) throw BusinessException.denied();
    return p;
  }
}
