package com.hajime.competition;

import com.hajime.shared.RecordSource;
import java.util.UUID;

/** No Judo scoring fields in neutral match contracts; discipline details belong behind policies. */
public final class CompetitionContracts {
  private CompetitionContracts() {}

  public record CompetitionEvent(UUID id, UUID martialArtId, String name) {}

  public record Match(UUID id, UUID competitionEventId) {}

  public record MatchParticipant(UUID matchId, UUID practitionerProfileId) {}

  public record MatchResult(
      UUID id, UUID matchId, UUID winnerPractitionerProfileId, RecordSource source) {}
}
