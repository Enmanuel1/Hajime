package com.hajime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
  @Test
  void moduleBoundariesAreAcyclic() {
    ApplicationModules.of(HajimeApplication.class).verify();
  }

  @Test
  void businessCodeDoesNotDependOnHttpTransport() {
    noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..application..")
        .or()
        .haveFullyQualifiedName("com.hajime.shared.BusinessException")
        .or()
        .haveFullyQualifiedName("com.hajime.shared.DomainValidation")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.http..", "org.springframework.web..", "jakarta.servlet..")
        .check(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.hajime"));
  }
}
