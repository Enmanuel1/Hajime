package com.hajime.martialart.domain.judo;

public final class JudoEvaluationPolicy implements com.hajime.martialart.EvaluationPolicy {
  public boolean eligible(boolean requiredCriteriaPassed, boolean evaluatorAuthorized) {
    return requiredCriteriaPassed && evaluatorAuthorized;
  }
}
