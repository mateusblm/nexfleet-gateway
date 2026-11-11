package br.com.nexfleet.gateway;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("br.com.nexfleet.gateway");

  @Test
  void naoImportaInternosDeOutrosServicos() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "br.com.nexfleet.identidade..",
            "br.com.nexfleet.frota..",
            "br.com.nexfleet.operacoes..",
            "br.com.nexfleet.indicadores..",
            "br.com.nexfleet.bff..")
        .check(classes);
  }
}
