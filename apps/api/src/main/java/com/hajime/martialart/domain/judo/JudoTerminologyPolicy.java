package com.hajime.martialart.domain.judo;

public final class JudoTerminologyPolicy implements com.hajime.martialart.TerminologyPolicy {
  public String term(String concept) {
    return java.util.Map.of(
            "organization",
            "Dojo",
            "instructor",
            "Sensei",
            "sparring",
            "Randori",
            "competition",
            "Shiai")
        .getOrDefault(concept, concept);
  }
}
