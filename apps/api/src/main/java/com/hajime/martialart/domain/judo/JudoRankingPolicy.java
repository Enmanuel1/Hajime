package com.hajime.martialart.domain.judo;

public final class JudoRankingPolicy implements com.hajime.martialart.RankingPolicy {
  public java.util.Optional<com.hajime.martialart.Catalog.Rank> nextRank(
      java.util.List<com.hajime.martialart.Catalog.Rank> ranks, java.util.UUID currentRankId) {
    var current =
        ranks.stream()
            .filter(r -> r.id().equals(currentRankId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown current rank"));
    return ranks.stream()
        .filter(
            r ->
                r.rankSystemId().equals(current.rankSystemId())
                    && r.orderIndex() > current.orderIndex())
        .min(java.util.Comparator.comparingInt(com.hajime.martialart.Catalog.Rank::orderIndex));
  }
}
