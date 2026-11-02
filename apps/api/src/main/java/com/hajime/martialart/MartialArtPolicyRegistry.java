package com.hajime.martialart;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MartialArtPolicyRegistry {
  private final Map<MartialArtCode, MartialArtPolicy> policies;

  public MartialArtPolicyRegistry(List<MartialArtPolicy> policies) {
    this.policies =
        policies.stream()
            .collect(Collectors.toUnmodifiableMap(MartialArtPolicy::code, Function.identity()));
  }

  public MartialArtPolicy resolve(MartialArtCode code) {
    var policy = policies.get(code);
    if (policy == null)
      throw new com.hajime.shared.ApiException(
          422,
          "MARTIAL_ART_NOT_SUPPORTED",
          "No behavior policies registered for this martial art.");
    return policy;
  }
}
