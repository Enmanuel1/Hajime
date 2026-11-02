package com.hajime.martialart.domain.judo;

public final class JudoCurriculumPolicy implements com.hajime.martialart.CurriculumPolicy {
  public boolean satisfies(
      java.util.Set<String> requiredCriteria, java.util.Set<String> demonstratedCriteria) {
    return demonstratedCriteria.containsAll(requiredCriteria);
  }
}
