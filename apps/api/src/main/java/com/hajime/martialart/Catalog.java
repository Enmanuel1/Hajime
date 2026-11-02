package com.hajime.martialart;

import com.hajime.martialart.infrastructure.CatalogRepository;
import com.hajime.shared.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Catalog {
  public record Art(UUID id, String code, String name) {}

  public record Rank(
      UUID id,
      UUID rankSystemId,
      String code,
      String displayName,
      int orderIndex,
      String beltColor) {}

  public record Technique(
      UUID id, UUID categoryId, String code, String name, String nativeName, String description) {}

  private final CatalogRepository repository;

  public Catalog(CatalogRepository repository) {
    this.repository = repository;
  }

  public List<Art> arts() {
    return repository.arts();
  }

  public Art art(String code) {
    return repository
        .art(new MartialArtCode(code).value())
        .orElseThrow(() -> BusinessException.missing("MARTIAL_ART_NOT_SUPPORTED"));
  }

  public Art art(UUID id) {
    return repository
        .art(id)
        .orElseThrow(() -> BusinessException.missing("MARTIAL_ART_NOT_SUPPORTED"));
  }

  public List<Rank> ranks(String code) {
    return repository.ranks(art(code).id());
  }

  public List<Technique> techniques(String code, int page, int size) {
    PageResult.limit(page, size);
    return repository.techniques(art(code).id(), page, size);
  }

  public void requireTechniques(UUID artId, Collection<UUID> techniqueIds) {
    if (techniqueIds.isEmpty()) return;
    var ids = Set.copyOf(techniqueIds);
    if (repository.activeTechniqueCount(artId, ids) != ids.size())
      throw BusinessException.invalid(
          "TECHNIQUE_MISMATCH",
          "Techniques must be active and belong to the selected martial art.");
  }

  public void requireTechnique(UUID artId, UUID techniqueId) {
    requireTechniques(artId, List.of(techniqueId));
  }
}
