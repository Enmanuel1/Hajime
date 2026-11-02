package com.hajime.martialart.domain.judo;

public final class JudoCompetitionPolicy implements com.hajime.martialart.CompetitionPolicy {
  public boolean validScore(String scoreCode) {
    return scoreCode != null && java.util.Set.of("IPPON", "WAZA_ARI", "YUKO").contains(scoreCode);
  }
}
