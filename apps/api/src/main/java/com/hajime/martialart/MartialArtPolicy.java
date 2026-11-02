package com.hajime.martialart;

public interface MartialArtPolicy {
  MartialArtCode code();

  RankingPolicy ranking();

  CurriculumPolicy curriculum();

  EvaluationPolicy evaluation();

  CompetitionPolicy competition();

  TerminologyPolicy terminology();
}
