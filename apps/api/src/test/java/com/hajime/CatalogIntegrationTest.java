package com.hajime;

import static org.assertj.core.api.Assertions.*;

import com.hajime.martialart.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogIntegrationTest extends ProductionPostgresTest {
  @Autowired Catalog catalog;
  @Autowired MartialArtPolicyRegistry policies;

  @Test
  void migrationsPopulateCatalogAndProgressionComesFromData() {
    var ranks = catalog.ranks("JUDO");
    assertThat(ranks).hasSize(7);
    assertThat(catalog.techniques("JUDO", 0, 100)).hasSize(11);
    assertThat(
            policies
                .resolve(new MartialArtCode("JUDO"))
                .ranking()
                .nextRank(ranks, ranks.getFirst().id()))
        .contains(ranks.get(1));
  }

  @Test
  void batchedValidationRequiresEveryTechniqueToBelongToTheArt() {
    var art = catalog.art("JUDO").id();
    var ids =
        catalog.techniques("JUDO", 0, 100).stream().limit(2).map(Catalog.Technique::id).toList();
    catalog.requireTechniques(art, ids);
    assertThatThrownBy(
            () ->
                catalog.requireTechniques(
                    art, java.util.List.of(ids.getFirst(), java.util.UUID.randomUUID())))
        .isInstanceOf(com.hajime.shared.BusinessException.class);
  }
}
