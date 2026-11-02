package com.hajime.martialart;

public interface CurriculumPolicy {
  boolean satisfies(
      java.util.Set<String> requiredCriteria, java.util.Set<String> demonstratedCriteria);
}
