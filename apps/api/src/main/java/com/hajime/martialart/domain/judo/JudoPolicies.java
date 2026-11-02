package com.hajime.martialart.domain.judo;

import com.hajime.martialart.*;
import org.springframework.stereotype.Component;

@Component
public class JudoPolicies implements MartialArtPolicy {
  public MartialArtCode code() {
    return new MartialArtCode("JUDO");
  }

  public RankingPolicy ranking() {
    return new JudoRankingPolicy();
  }

  public CurriculumPolicy curriculum() {
    return new JudoCurriculumPolicy();
  }

  public EvaluationPolicy evaluation() {
    return new JudoEvaluationPolicy();
  }

  public CompetitionPolicy competition() {
    return new JudoCompetitionPolicy();
  }

  public TerminologyPolicy terminology() {
    return new JudoTerminologyPolicy();
  }
}
