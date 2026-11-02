package com.hajime;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
  @Test
  void moduleBoundariesAreAcyclic() {
    ApplicationModules.of(HajimeApplication.class).verify();
  }
}
