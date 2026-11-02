package com.hajime.martialart.api;

import com.hajime.martialart.Catalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/martial-arts")
public class CatalogController {
  private final Catalog catalog;

  public CatalogController(Catalog catalog) {
    this.catalog = catalog;
  }

  @GetMapping
  public List<Catalog.Art> arts() {
    return catalog.arts();
  }

  @GetMapping("/{code}/ranks")
  public List<Catalog.Rank> ranks(@PathVariable String code) {
    return catalog.ranks(code);
  }

  @GetMapping("/{code}/techniques")
  public List<Catalog.Technique> techniques(
      @PathVariable String code,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    return catalog.techniques(code, page, size);
  }
}
