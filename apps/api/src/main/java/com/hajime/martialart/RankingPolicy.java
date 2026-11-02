package com.hajime.martialart;

public interface RankingPolicy {
  java.util.Optional<com.hajime.martialart.Catalog.Rank> nextRank(
      java.util.List<com.hajime.martialart.Catalog.Rank> ranks, java.util.UUID currentRankId);
}
