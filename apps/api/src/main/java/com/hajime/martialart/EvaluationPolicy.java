package com.hajime.martialart;

public interface EvaluationPolicy {
  boolean eligible(boolean requiredCriteriaPassed, boolean evaluatorAuthorized);
}
